package PUhr.data.repository

import PUhr.core.crypto.KeyDeriver
import PUhr.core.crypto.SaltGenerator
import PUhr.core.session.SessionManager
import PUhr.domain.repository.AuthRepository
import PUhr.domain.repository.SetupResult
import PUhr.domain.repository.UnlockResult
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val context: Context,
    private val keyDeriver: KeyDeriver,
    private val saltGenerator: SaltGenerator,
    private val sessionManager: SessionManager,
) : AuthRepository {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override suspend fun isFirstTimeSetup(): Boolean = !prefs.contains(KEY_SALT)

    override suspend fun setup(pin: String): SetupResult {
        if (!isFirstTimeSetup()) return SetupResult.AlreadySetup
        val salt = saltGenerator.generate()
        val derived = keyDeriver.derive(pin, salt)
        val hash = hashDerived(derived)
        prefs.edit()
            .putString(KEY_SALT, encodeBase64(salt))
            .putString(KEY_HASH, encodeBase64(hash))
            .putInt(KEY_FAIL_COUNT, 0)
            .putLong(KEY_LAST_FAIL_TIMESTAMP, 0L)
            .apply()
        val vek = derived.copyOfRange(0, 32)
        val dbk = derived.copyOfRange(32, 64)
        sessionManager.openSession(vek, dbk)
        return SetupResult.Success(dbk)
    }

    override suspend fun unlock(pin: String): UnlockResult {
        if (isFirstTimeSetup()) return UnlockResult.NotSetup
        val salt = decodeBase64(prefs.getString(KEY_SALT, null) ?: return UnlockResult.NotSetup)
        val storedHash = decodeBase64(prefs.getString(KEY_HASH, null)
            ?: return UnlockResult.NotSetup)
        var failCount = prefs.getInt(KEY_FAIL_COUNT, 0)

        if (failCount >= PANIC_THRESHOLD) return UnlockResult.PanicWipeTriggered

        if (failCount >= THROTTLE_START) {
            val remaining = remainingCooldownFor(failCount)
            if (remaining > 0) return UnlockResult.Throttled(remaining.toInt())
            prefs.edit().putInt(KEY_FAIL_COUNT, THROTTLE_START - 1).apply()
            failCount = THROTTLE_START - 1
        }

        val derived = keyDeriver.derive(pin, salt)
        val hash = hashDerived(derived)

        if (MessageDigest.isEqual(storedHash, hash)) {
            prefs.edit()
                .putInt(KEY_FAIL_COUNT, 0)
                .putLong(KEY_LAST_FAIL_TIMESTAMP, 0L)
                .apply()
            val vek = derived.copyOfRange(0, 32)
            val dbk = derived.copyOfRange(32, 64)
            sessionManager.openSession(vek, dbk)
            return UnlockResult.Success(dbk)
        }

        val newFailCount = failCount + 1
        prefs.edit()
            .putInt(KEY_FAIL_COUNT, newFailCount)
            .putLong(KEY_LAST_FAIL_TIMESTAMP, Instant.now().toEpochMilli())
            .apply()

        if (newFailCount >= PANIC_THRESHOLD) return UnlockResult.PanicWipeTriggered
        if (newFailCount >= THROTTLE_START) {
            return UnlockResult.Throttled(cooldownSecondsFor(newFailCount).toInt())
        }
        return UnlockResult.WrongPin
    }

    override suspend fun changePin(oldPin: String, newPin: String): Result<Unit> {
        if (isFirstTimeSetup()) return Result.failure(IllegalStateException("Not setup"))
        val salt = decodeBase64(prefs.getString(KEY_SALT, null)
            ?: return Result.failure(IllegalStateException("Not setup")))
        val storedHash = decodeBase64(prefs.getString(KEY_HASH, null)
            ?: return Result.failure(IllegalStateException("Not setup")))
        val oldDerived = keyDeriver.derive(oldPin, salt)
        if (!MessageDigest.isEqual(storedHash, hashDerived(oldDerived))) {
            return Result.failure(IllegalArgumentException("Wrong PIN"))
        }
        val newSalt = saltGenerator.generate()
        val newDerived = keyDeriver.derive(newPin, newSalt)
        val newHash = hashDerived(newDerived)
        prefs.edit()
            .putString(KEY_SALT, encodeBase64(newSalt))
            .putString(KEY_HASH, encodeBase64(newHash))
            .putInt(KEY_FAIL_COUNT, 0)
            .putLong(KEY_LAST_FAIL_TIMESTAMP, 0L)
            .apply()
        return Result.success(Unit)
    }

    override suspend fun remainingCooldown(): Duration {
        val failCount = prefs.getInt(KEY_FAIL_COUNT, 0)
        if (failCount < THROTTLE_START) return Duration.ZERO
        return Duration.ofSeconds(remainingCooldownFor(failCount))
    }

    override suspend fun failCount(): Int = prefs.getInt(KEY_FAIL_COUNT, 0)

    private fun remainingCooldownFor(failCount: Int): Long {
        val lastFail = prefs.getLong(KEY_LAST_FAIL_TIMESTAMP, 0L)
        if (lastFail == 0L) return 0L
        val elapsed = Instant.now().toEpochMilli() - lastFail
        val cooldown = cooldownSecondsFor(failCount) * 1000L
        val remaining = cooldown - elapsed
        return if (remaining > 0) remaining / 1000L else 0L
    }

    private fun cooldownSecondsFor(failCount: Int): Long {
        val exponent = failCount - THROTTLE_START
        var cooldown = THROTTLE_BASE_SECONDS
        repeat(exponent) { cooldown *= 2 }
        return minOf(cooldown, MAX_COOLDOWN_SECONDS)
    }

    private fun hashDerived(derived: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(derived)

    private fun encodeBase64(data: ByteArray): String =
        Base64.getEncoder().encodeToString(data)

    private fun decodeBase64(str: String): ByteArray =
        Base64.getDecoder().decode(str)

    companion object {
        private const val PREFS_NAME = "uhr_auth_prefs"
        private const val KEY_SALT = "salt"
        private const val KEY_HASH = "hash"
        private const val KEY_FAIL_COUNT = "fail_count"
        private const val KEY_LAST_FAIL_TIMESTAMP = "last_fail_timestamp"
        private const val THROTTLE_BASE_SECONDS = 30L
        private const val MAX_COOLDOWN_SECONDS = 600L
        private const val THROTTLE_START = 5
        private const val PANIC_THRESHOLD = 15
    }
}
