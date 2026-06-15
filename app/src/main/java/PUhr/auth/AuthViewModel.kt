package PUhr.auth

import PUhr.domain.repository.AuthRepository
import PUhr.domain.repository.SetupResult
import PUhr.domain.repository.UnlockResult
import PUhr.domain.usecase.auth.ChangePinUseCase
import PUhr.domain.usecase.auth.UnlockVaultUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    data object Loading : AuthUiState()
    data class FirstTimeSetup(val dotsCount: Int = 0) : AuthUiState()
    data class PinEntry(val dotsCount: Int = 0) : AuthUiState()
    data object Authenticating : AuthUiState()
    data class Verified(val dbk: ByteArray) : AuthUiState()
    data class Throttled(val remainingSeconds: Int) : AuthUiState()
    data class Error(val isPinMismatch: Boolean = false) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val unlockVaultUseCase: UnlockVaultUseCase,
    private val changePinUseCase: ChangePinUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var pendingSetupPin: String? = null

    init {
        checkSetupState()
    }

    private fun checkSetupState() {
        viewModelScope.launch {
            _uiState.value = if (authRepository.isFirstTimeSetup()) {
                AuthUiState.FirstTimeSetup()
            } else {
                AuthUiState.PinEntry()
            }
        }
    }

    fun onDigit(digit: Char) {
        val state = _uiState.value
        _uiState.value = when (state) {
            is AuthUiState.PinEntry -> state.copy(dotsCount = state.dotsCount + 1)
            is AuthUiState.FirstTimeSetup -> state.copy(dotsCount = state.dotsCount + 1)
            else -> return
        }
    }

    fun onBackspace() {
        val state = _uiState.value
        val newCount = ((state as? AuthUiState.PinEntry)?.dotsCount
            ?: (state as? AuthUiState.FirstTimeSetup)?.dotsCount
            ?: return) - 1
        _uiState.value = when (state) {
            is AuthUiState.PinEntry -> state.copy(dotsCount = newCount.coerceAtLeast(0))
            is AuthUiState.FirstTimeSetup -> state.copy(dotsCount = newCount.coerceAtLeast(0))
            else -> return
        }
    }

    fun onSubmit(pin: String) {
        if (pin.isEmpty()) return
        viewModelScope.launch {
            val state = _uiState.value
            when {
                state is AuthUiState.FirstTimeSetup -> handleSetup(pin)
                state is AuthUiState.PinEntry -> handleUnlock(pin)
            }
        }
    }

    private suspend fun handleSetup(pin: String) {
        _uiState.value = AuthUiState.Authenticating
        when (val result = authRepository.setup(pin)) {
            is SetupResult.Success -> {
                _uiState.value = AuthUiState.Verified(dbk = result.dbk)
            }
            is SetupResult.AlreadySetup -> {
                _uiState.value = AuthUiState.PinEntry()
            }
        }
    }

    private suspend fun handleUnlock(pin: String) {
        _uiState.value = AuthUiState.Authenticating
        when (val result = unlockVaultUseCase(pin)) {
            is UnlockResult.Success -> {
                _uiState.value = AuthUiState.Verified(dbk = result.dbk)
            }
            is UnlockResult.WrongPin -> {
                _uiState.value = AuthUiState.Error(isPinMismatch = true)
                delay(600L)
                _uiState.value = AuthUiState.PinEntry()
            }
            is UnlockResult.Throttled -> {
                _uiState.value = AuthUiState.Throttled(
                    remainingSeconds = result.remainingSeconds,
                )
            }
            is UnlockResult.PanicWipeTriggered -> {
                _uiState.value = AuthUiState.Error(isPinMismatch = false)
            }
            is UnlockResult.NotSetup -> {
                _uiState.value = AuthUiState.FirstTimeSetup()
            }
        }
    }

    fun onThrottleComplete() {
        if (_uiState.value is AuthUiState.Throttled) {
            _uiState.value = AuthUiState.PinEntry()
        }
    }

    fun onErrorDismissed() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.PinEntry()
        }
    }
}
