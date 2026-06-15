package PUhr.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

@Composable
fun AuthScreen(
    onVerified: (ByteArray) -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pinBuffer by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0C))
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (val state = uiState) {
            is AuthUiState.Loading -> {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            is AuthUiState.FirstTimeSetup -> {
                AuthContent(
                    title = "Create PIN",
                    subtitle = "Choose a vault unlock PIN",
                    dotsCount = state.dotsCount,
                    shake = false,
                    enabled = true,
                    onPinEvent = { event ->
                        when (event) {
                            is PinEvent.Digit -> {
                                pinBuffer += event.digit
                                viewModel.onDigit(event.digit)
                            }
                            is PinEvent.Backspace -> {
                                pinBuffer = pinBuffer.dropLast(1)
                                viewModel.onBackspace()
                            }
                            is PinEvent.Submit -> {
                                viewModel.onSubmit(pinBuffer)
                                pinBuffer = ""
                            }
                        }
                    },
                )
            }

            is AuthUiState.PinEntry -> {
                AuthContent(
                    title = "Enter PIN",
                    subtitle = "",
                    dotsCount = state.dotsCount,
                    shake = false,
                    enabled = true,
                    onPinEvent = { event ->
                        when (event) {
                            is PinEvent.Digit -> {
                                pinBuffer += event.digit
                                viewModel.onDigit(event.digit)
                            }
                            is PinEvent.Backspace -> {
                                pinBuffer = pinBuffer.dropLast(1)
                                viewModel.onBackspace()
                            }
                            is PinEvent.Submit -> {
                                viewModel.onSubmit(pinBuffer)
                                pinBuffer = ""
                            }
                        }
                    },
                )
            }

            is AuthUiState.Authenticating -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Verifying...",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                    )
                }
            }

            is AuthUiState.Verified -> {
                LaunchedEffect(Unit) {
                    onVerified(state.dbk)
                }
            }

            is AuthUiState.Throttled -> {
                ThrottledMessage(remainingSeconds = state.remainingSeconds) {
                    viewModel.onThrottleComplete()
                }
            }

            is AuthUiState.Error -> {
                if (state.isPinMismatch) {
                    AuthContent(
                        title = "Enter PIN",
                        subtitle = "Wrong PIN",
                        dotsCount = 0,
                        shake = true,
                        enabled = false,
                        onPinEvent = {},
                    )
                    LaunchedEffect(Unit) {
                        delay(600L)
                        viewModel.onErrorDismissed()
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "Too many attempts",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Vault has been wiped for security.",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthContent(
    title: String,
    subtitle: String,
    dotsCount: Int,
    shake: Boolean,
    enabled: Boolean,
    onPinEvent: (PinEvent) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )

        if (subtitle.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        PinPad(
            dotsCount = dotsCount,
            shake = shake,
            enabled = enabled,
            onDigit = { onPinEvent(PinEvent.Digit(it)) },
            onBackspace = { onPinEvent(PinEvent.Backspace) },
            onSubmit = { onPinEvent(PinEvent.Submit) },
        )
    }
}

@Composable
private fun ThrottledMessage(
    remainingSeconds: Int,
    onComplete: () -> Unit,
) {
    var seconds by remember { mutableStateOf(remainingSeconds) }

    LaunchedEffect(remainingSeconds) {
        seconds = remainingSeconds
        while (seconds > 0) {
            delay(1000L)
            seconds--
        }
        onComplete()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = seconds.toString(),
                color = MaterialTheme.colorScheme.error,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Too many attempts",
            color = MaterialTheme.colorScheme.error,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Try again in ${seconds}s",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontSize = 14.sp,
        )
    }
}

sealed class PinEvent {
    data class Digit(val digit: Char) : PinEvent()
    data object Backspace : PinEvent()
    data object Submit : PinEvent()
}
