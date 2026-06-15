package PUhr.auth

import PUhr.core.crypto.FakeKeyDeriver
import PUhr.core.crypto.SaltGenerator
import PUhr.domain.repository.AuthRepository
import PUhr.domain.repository.SetupResult
import PUhr.domain.repository.UnlockResult
import PUhr.domain.usecase.auth.ChangePinUseCase
import PUhr.domain.usecase.auth.UnlockVaultUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeAuthRepo
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeAuthRepo()
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isFirstTimeSetup() {
        assert(viewModel.uiState.value is AuthUiState.FirstTimeSetup)
    }

    @Test
    fun afterSetup_returnsVerified() = runTest(testDispatcher) {
        viewModel.onSubmit("1234")
        testDispatcher.scheduler.advanceUntilIdle()
        assert(viewModel.uiState.value is AuthUiState.Verified)
    }

    @Test
    fun correctUnlock_returnsVerified() = runTest(testDispatcher) {
        fakeRepo.setupSync("1234")
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assert(viewModel.uiState.value is AuthUiState.PinEntry)

        viewModel.onSubmit("1234")
        testDispatcher.scheduler.advanceUntilIdle()
        assert(viewModel.uiState.value is AuthUiState.Verified)
    }

    @Test
    fun wrongPin_showsErrorThenPinEntry() = runTest(testDispatcher) {
        fakeRepo.setupSync("1234")
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSubmit("wrong")
        testDispatcher.scheduler.advanceTimeBy(1)
        assert(viewModel.uiState.value is AuthUiState.Error)

        testDispatcher.scheduler.advanceTimeBy(600)
        assert(viewModel.uiState.value is AuthUiState.PinEntry)
    }

    @Test
    fun throttleStateAfter5Fails() = runTest(testDispatcher) {
        fakeRepo.setupSync("1234")
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        for (i in 1..5) {
            viewModel.onSubmit("wrong")
            testDispatcher.scheduler.advanceTimeBy(1)
            testDispatcher.scheduler.advanceTimeBy(700)
        }
        assert(viewModel.uiState.value is AuthUiState.Throttled)
    }

    @Test
    fun pinEntry_onDigit_updatesDots() = runTest(testDispatcher) {
        fakeRepo.setupSync("1234")
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onDigit('1')
        val state = viewModel.uiState.value as AuthUiState.PinEntry
        assert(state.dotsCount == 1)
    }

    @Test
    fun pinEntry_onBackspace_reducesDots() = runTest(testDispatcher) {
        fakeRepo.setupSync("1234")
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onDigit('1')
        viewModel.onDigit('2')
        viewModel.onBackspace()
        val state = viewModel.uiState.value as AuthUiState.PinEntry
        assert(state.dotsCount == 1)
    }

    private fun createViewModel(): AuthViewModel {
        val unlockUseCase = UnlockVaultUseCase(fakeRepo)
        val changePinUseCase = ChangePinUseCase(fakeRepo)
        return AuthViewModel(
            authRepository = fakeRepo,
            unlockVaultUseCase = unlockUseCase,
            changePinUseCase = changePinUseCase,
        )
    }
}

private class FakeAuthRepo : AuthRepository {

    private data class VaultData(
        val salt: ByteArray,
        val hash: ByteArray,
        var failCount: Int = 0,
        var lastFailTimestamp: Long = 0L,
    )

    private var vault: VaultData? = null
    private val keyDeriver = FakeKeyDeriver()
    private val saltGenerator = SaltGenerator()

    fun setupSync(pin: String) {
        val salt = saltGenerator.generate()
        val derived = keyDeriver.derive(pin, salt)
        val hash = MessageDigest.getInstance("SHA-256").digest(derived)
        vault = VaultData(salt = salt, hash = hash)
    }

    override suspend fun isFirstTimeSetup(): Boolean = vault == null

    override suspend fun setup(pin: String): SetupResult {
        if (vault != null) return SetupResult.AlreadySetup
        val salt = saltGenerator.generate()
        val derived = keyDeriver.derive(pin, salt)
        val hash = MessageDigest.getInstance("SHA-256").digest(derived)
        vault = VaultData(salt = salt, hash = hash)
        val dbk = derived.copyOfRange(32, 64)
        return SetupResult.Success(dbk)
    }

    override suspend fun unlock(pin: String): UnlockResult {
        val data = vault ?: return UnlockResult.NotSetup

        if (data.failCount >= PANIC_THRESHOLD) return UnlockResult.PanicWipeTriggered

        if (data.failCount >= THROTTLE_START) {
            if (data.lastFailTimestamp != 0L) {
                val elapsed = Instant.now().toEpochMilli() - data.lastFailTimestamp
                val cooldown = cooldownFor(data.failCount) * 1000L
                if (elapsed < cooldown) {
                    return UnlockResult.Throttled(
                        ((cooldown - elapsed) / 1000L).toInt().coerceAtLeast(1),
                    )
                }
            }
            data.failCount = THROTTLE_START - 1
        }

        val derived = keyDeriver.derive(pin, data.salt)
        val hash = MessageDigest.getInstance("SHA-256").digest(derived)

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
        if (!MessageDigest.isEqual(data.hash,
                MessageDigest.getInstance("SHA-256").digest(oldDerived))) {
            return Result.failure(IllegalArgumentException("Wrong PIN"))
        }
        val newSalt = saltGenerator.generate()
        val newDerived = keyDeriver.derive(newPin, newSalt)
        val newHash = MessageDigest.getInstance("SHA-256").digest(newDerived)
        vault = VaultData(salt = newSalt, hash = newHash)
        return Result.success(Unit)
    }

    override suspend fun remainingCooldown(): Duration {
        val data = vault ?: return Duration.ZERO
        if (data.failCount < THROTTLE_START || data.lastFailTimestamp == 0L) return Duration.ZERO
        val elapsed = Instant.now().toEpochMilli() - data.lastFailTimestamp
        val cooldown = cooldownFor(data.failCount) * 1000L
        return Duration.ofSeconds(((cooldown - elapsed) / 1000L).coerceAtLeast(0))
    }

    override suspend fun failCount(): Int = vault?.failCount ?: 0

    companion object {
        private const val THROTTLE_BASE_SECONDS = 30L
        private const val MAX_COOLDOWN_SECONDS = 600L
        private const val THROTTLE_START = 5
        private const val PANIC_THRESHOLD = 15

        private fun cooldownFor(failCount: Int): Long {
            val exponent = failCount - THROTTLE_START
            var cooldown = THROTTLE_BASE_SECONDS
            repeat(exponent) { cooldown *= 2 }
            return minOf(cooldown, MAX_COOLDOWN_SECONDS)
        }
    }
}
