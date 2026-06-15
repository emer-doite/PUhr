package PUhr.domain.repository

import java.time.Duration

sealed class SetupResult {
    data class Success(val dbk: ByteArray) : SetupResult()
    data object AlreadySetup : SetupResult()
}

sealed class UnlockResult {
    data class Success(val dbk: ByteArray) : UnlockResult()
    data object WrongPin : UnlockResult()
    data class Throttled(val remainingSeconds: Int) : UnlockResult()
    data object PanicWipeTriggered : UnlockResult()
    data object NotSetup : UnlockResult()
}

interface AuthRepository {
    suspend fun isFirstTimeSetup(): Boolean
    suspend fun setup(pin: String): SetupResult
    suspend fun unlock(pin: String): UnlockResult
    suspend fun changePin(oldPin: String, newPin: String): Result<Unit>
    suspend fun remainingCooldown(): Duration
    suspend fun failCount(): Int
}
