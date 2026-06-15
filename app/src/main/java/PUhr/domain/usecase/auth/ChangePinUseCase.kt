package PUhr.domain.usecase.auth

import PUhr.domain.repository.AuthRepository
import javax.inject.Inject

class ChangePinUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(oldPin: String, newPin: String): Result<Unit> {
        return authRepository.changePin(oldPin, newPin)
    }
}
