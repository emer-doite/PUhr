package PUhr.data.auth

import PUhr.core.crypto.FakeKeyDeriver
import PUhr.core.crypto.KeyDeriver
import PUhr.core.crypto.SaltGenerator
import PUhr.domain.repository.AuthRepository
import PUhr.domain.repository.SetupResult
import PUhr.domain.repository.UnlockResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant

class AuthRepositoryTest {

    private val keyDeriver = FakeKeyDeriver()
    private val saltGenerator = SaltGenerator()
    private val repo = TestAuthRepository(keyDeriver, saltGenerator)

    @Test
    fun firstTimeSetup_returnsTrue() = runTest {
        assertTrue(repo.isFirstTimeSetup())
    }

    @Test
    fun setupWithPin_returnsSuccessWithDbk() = runTest {
        val result = repo.setup("1234")
        val success = result as? SetupResult.Success ?: error("Expected Success")
        assertEquals(32, success.dbk.size)
    }

    @Test
    fun setupTwice_returnsAlreadySetup() = runTest {
        repo.setup("1234")
        val result = repo.setup("5678")
        assertTrue(result is SetupResult.AlreadySetup)
    }

    @Test
    fun correctPinAfterSetup_returnsSuccess() = runTest {
        repo.setup("1234")
        val result = repo.unlock("1234")
        assertTrue(result is UnlockResult.Success)
    }

    @Test
    fun wrongPin_returnsWrongPin() = runTest {
        repo.setup("1234")
        val result = repo.unlock("9999")
        assertTrue(result is UnlockResult.WrongPin)
    }

    @Test
    fun unlockBeforeSetup_returnsNotSetup() = runTest {
        val result = repo.unlock("1234")
        assertTrue(result is UnlockResult.NotSetup)
    }

    @Test
    fun throttleActivatesAt5Failures() = runTest {
        repo.setup("1234")
        for (i in 1..4) {
            assertTrue("Attempt $i should be WrongPin", repo.unlock("wrong") is UnlockResult.WrongPin)
        }
        val r5 = repo.unlock("wrong")
        assertTrue("Attempt 5 should be Throttled, got $r5", r5 is UnlockResult.Throttled)
        val throttled = r5 as UnlockResult.Throttled
        assertTrue("Cooldown should be <= 30s, got ${throttled.remainingSeconds}", throttled.remainingSeconds <= 30)
        assertTrue("Cooldown should be > 0", throttled.remainingSeconds > 0)
    }

    @Test
    fun cooldownDoublesOnSubsequentThrottledAttempts() = runTest {
        repo.setup("1234")
        for (i in 1..5) {
            repo.unlock("wrong")
        }
        val attempts = mutableListOf<Int>()
        for (i in 0..2) {
            val r = repo.unlock("wrong")
            if (r is UnlockResult.Throttled) {
                attempts.add(r.remainingSeconds)
            }
        }
        assertTrue("Expected at least 2 Throttled results", attempts.size >= 2)
        for (i in 1 until attempts.size) {
            assertTrue("Cooldown should increase: ${attempts[i-1]} -> ${attempts[i]}", attempts[i] >= attempts[i-1])
        }
    }

    @Test
    fun correctPinResetsFailCount() = runTest {
        repo.setup("1234")
        repo.unlock("wrong")
        repo.unlock("wrong")
        assertTrue(repo.failCount() == 2)
        repo.unlock("1234")
        assertEquals(0, repo.failCount())
    }

    @Test
    fun failCountPersistsAcrossCalls() = runTest {
        repo.setup("1234")
        assertEquals(0, repo.failCount())
        repo.unlock("wrong")
        assertEquals(1, repo.failCount())
        repo.unlock("wrong")
        assertEquals(2, repo.failCount())
    }

    @Test
    fun changePin_oldPinRequired() = runTest {
        repo.setup("1234")
        assertTrue(repo.changePin("wrong", "5678").isFailure)
    }

    @Test
    fun changePin_updatesPin() = runTest {
        repo.setup("1234")
        repo.changePin("1234", "5678")
        assertTrue(repo.unlock("5678") is UnlockResult.Success)
        assertTrue(repo.unlock("1234") is UnlockResult.WrongPin)
    }

    @Test
    fun remainingCooldown_zeroWhenNotThrottled() = runTest {
        repo.setup("1234")
        assertEquals(Duration.ZERO, repo.remainingCooldown())
    }

    @Test
    fun remainingCooldown_nonZeroWhenThrottled() = runTest {
        repo.setup("1234")
        for (i in 1..5) {
            repo.unlock("wrong")
        }
        assertTrue(repo.remainingCooldown() > Duration.ZERO)
    }

    @Test
    fun cooldownFormula_startingAt30s() {
        assertEquals(30L, TestAuthRepository.cooldownFor(5))
        assertEquals(60L, TestAuthRepository.cooldownFor(6))
        assertEquals(120L, TestAuthRepository.cooldownFor(7))
        assertEquals(240L, TestAuthRepository.cooldownFor(8))
        assertEquals(480L, TestAuthRepository.cooldownFor(9))
        assertEquals(600L, TestAuthRepository.cooldownFor(10))
        assertEquals(600L, TestAuthRepository.cooldownFor(15))
    }
}

internal class TestAuthRepository(
    private val keyDeriver: KeyDeriver,
    private val saltGenerator: SaltGenerator,
) : AuthRepository {

    private data class VaultData(
        val salt: ByteArray,
        val hash: ByteArray,
        var failCount: Int = 0,
        var lastFailTimestamp: Long = 0L,
    )

    private var vault: VaultData? = null

    override suspend fun isFirstTimeSetup(): Boolean = vault == null

    override suspend fun setup(pin: String): SetupResult {
        if (vault != null) return SetupResult.AlreadySetup
        val salt = saltGenerator.generate()
        val derived = keyDeriver.derive(pin, salt)
        val hash = hashDerived(derived)
        vault = VaultData(salt = salt, hash = hash)
        val dbk = derived.copyOfRange(32, 64)
        return SetupResult.Success(dbk)
    }

    override suspend fun unlock(pin: String): UnlockResult {
        val data = vault ?: return UnlockResult.NotSetup

        if (data.failCount >= PANIC_THRESHOLD) return UnlockResult.PanicWipeTriggered

        if (data.failCount >= THROTTLE_START) {
            val remaining = remainingCooldownFor(data)
            if (remaining > 0) return UnlockResult.Throttled(remaining.toInt())
            data.failCount = THROTTLE_START - 1
        }

        val derived = keyDeriver.derive(pin, data.salt)
        val hash = hashDerived(derived)

        if (MessageDigest.isEqual(data.hash, hash)) {
            data.failCount = 0
            data.lastFailTimestamp = 0L
            val dbk = derived.copyOfRange(32, 64)
            return UnlockResult.Success(dbk)
        }

        data.failCount++
        data.lastFailTimestamp = Instant.now().toEpochMilli()

        if (data.failCount >= PANIC_THRESHOLD) return UnlockResult.PanicWipeTriggered
        if (data.failCount >= THROTTLE_START) {
            return UnlockResult.Throttled(cooldownFor(data.failCount).toInt())
        }
        return UnlockResult.WrongPin
    }

    override suspend fun changePin(oldPin: String, newPin: String): Result<Unit> {
        val data = vault ?: return Result.failure(IllegalStateException("Not setup"))
        val oldDerived = keyDeriver.derive(oldPin, data.salt)
        if (!MessageDigest.isEqual(data.hash, hashDerived(oldDerived))) {
            return Result.failure(IllegalArgumentException("Wrong PIN"))
        }
        val newSalt = saltGenerator.generate()
        val newDerived = keyDeriver.derive(newPin, newSalt)
        val newHash = hashDerived(newDerived)
        vault = VaultData(salt = newSalt, hash = newHash)
        return Result.success(Unit)
    }

    override suspend fun remainingCooldown(): Duration {
        val data = vault ?: return Duration.ZERO
        if (data.failCount < THROTTLE_START) return Duration.ZERO
        return Duration.ofSeconds(remainingCooldownFor(data))
    }

    override suspend fun failCount(): Int = vault?.failCount ?: 0

    private fun hashDerived(derived: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(derived)

    private fun remainingCooldownFor(data: VaultData): Long {
        if (data.lastFailTimestamp == 0L) return 0L
        val elapsed = Instant.now().toEpochMilli() - data.lastFailTimestamp
        val cooldown = cooldownFor(data.failCount) * 1000L
        val remaining = cooldown - elapsed
        return if (remaining > 0) remaining / 1000L + 1 else 0L
    }

    companion object {
        private const val THROTTLE_BASE_SECONDS = 30L
        private const val MAX_COOLDOWN_SECONDS = 600L
        const val THROTTLE_START = 5
        const val PANIC_THRESHOLD = 15

        fun cooldownFor(failCount: Int): Long {
            val exponent = failCount - THROTTLE_START
            var cooldown = THROTTLE_BASE_SECONDS
            repeat(exponent) { cooldown *= 2 }
            return minOf(cooldown, MAX_COOLDOWN_SECONDS)
        }
    }
}
