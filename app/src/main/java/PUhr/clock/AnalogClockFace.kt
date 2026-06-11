package PUhr.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.sin

fun Float.toHandDegrees(totalUnits: Int): Float =
    this / totalUnits * 360f

data class HandAngles(
    val hour: Float,
    val minute: Float,
    val second: Float,
)

fun calculateClockAngles(time: LocalTime): HandAngles {
    val hour = (time.hour % 12) * 30f + time.minute * 0.5f
    val minute = time.minute * 6f + time.second * 0.1f
    val second = time.second * 6f + time.nano / 1_000_000_000f * 6f
    return HandAngles(hour = hour, minute = minute, second = second)
}

@Composable
fun AnalogClockFace(
    hours: Float,
    minutes: Float,
    seconds: Float,
    modifier: Modifier = Modifier,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val accentColor = Color(0xFFCF6679)

    Canvas(modifier = modifier.fillMaxSize().aspectRatio(1f)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f

        drawCircle(color = surfaceColor, radius = radius, center = center)
        drawCircle(
            color = onSurfaceColor,
            radius = radius,
            center = center,
            style = Stroke(width = 4f),
        )

        for (i in 0 until 12) {
            val rad = (i * 30f) * (kotlin.math.PI / 180f).toFloat()
            val innerR = radius * 0.85f
            val outerR = radius * 0.95f
            drawLine(
                color = onSurfaceColor,
                start = Offset(center.x + innerR * cos(rad), center.y + innerR * sin(rad)),
                end = Offset(center.x + outerR * cos(rad), center.y + outerR * sin(rad)),
                strokeWidth = 4f,
            )
        }

        rotate(hours, center) {
            drawRect(
                color = onSurfaceColor,
                topLeft = Offset(center.x - 4f, center.y - radius * 0.5f),
                size = Size(8f, radius * 0.5f),
            )
        }

        rotate(minutes, center) {
            drawRect(
                color = onSurfaceColor,
                topLeft = Offset(center.x - 3f, center.y - radius * 0.7f),
                size = Size(6f, radius * 0.7f),
            )
        }

        rotate(seconds, center) {
            drawRect(
                color = accentColor,
                topLeft = Offset(center.x - 1.5f, center.y - radius * 0.85f),
                size = Size(3f, radius * 0.85f),
            )
        }

        drawCircle(color = onSurfaceColor, radius = 8f, center = center)
    }
}
