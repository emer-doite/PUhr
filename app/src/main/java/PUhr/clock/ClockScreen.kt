package PUhr.clock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalTime
import kotlinx.coroutines.delay
import PUhr.clock.gesture.GestureConfig
import PUhr.clock.gesture.secretGestureDetector

fun LocalTime.formatHMS(): String =
    this.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))

@Composable
fun ClockScreen(
    onTriggerDetected: () -> Unit = {},
    viewModel: ClockViewModel = hiltViewModel(),
) {
    var digitalTime by remember { mutableStateOf("") }
    var hourAngle by remember { mutableFloatStateOf(0f) }
    var minuteAngle by remember { mutableFloatStateOf(0f) }
    var secondAngle by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis {
                val angles = calculateClockAngles(LocalTime.now())
                hourAngle = angles.hour
                minuteAngle = angles.minute
                secondAngle = angles.second
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            digitalTime = LocalTime.now().formatHMS()
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .secretGestureDetector(GestureConfig(), onTriggerDetected),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnalogClockFace(
            hours = hourAngle,
            minutes = minuteAngle,
            seconds = secondAngle,
        )

        Text(
            text = digitalTime,
            fontFamily = FontFamily.Monospace,
            fontSize = 48.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
