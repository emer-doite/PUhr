package PUhr.domain.usecase.auth

import PUhr.domain.repository.AuthRepository
import PUhr.domain.repository.UnlockResult
import javax.inject.Inject

class UnlockVaultUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(pin: String): UnlockResult {
        return authRepository.unlock(pin)
    }
}
