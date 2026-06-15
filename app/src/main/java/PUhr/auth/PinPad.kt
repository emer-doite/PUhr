package PUhr.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import kotlinx.coroutines.delay

@Composable
fun PinPad(
    dotsCount: Int,
    shake: Boolean,
    enabled: Boolean,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(shake) {
        if (shake) {
            repeat(3) {
                shakeOffset.animateTo(10f, tween(50))
                shakeOffset.animateTo(-10f, tween(50))
            }
            shakeOffset.animateTo(0f, tween(50))
        }
    }

    Column(
        modifier = modifier
            .offset(x = shakeOffset.value.dp)
            .semantics { testTag = "PinPad" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DotsRow(dotsCount = dotsCount)

        Spacer(modifier = Modifier.height(32.dp))

        KeypadGrid(
            dotsCount = dotsCount,
            enabled = enabled,
            onDigit = onDigit,
            onBackspace = onBackspace,
            onSubmit = onSubmit,
        )
    }
}

@Composable
private fun DotsRow(dotsCount: Int) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .semantics { testTag = "DotsRow:$dotsCount" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until dotsCount) {
            val scale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "dotScale",
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .scale(scale)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface),
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
    }
}

@Composable
private fun KeypadGrid(
    dotsCount: Int,
    enabled: Boolean,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        for (row in 0..2) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            ) {
                for (col in 0..2) {
                    val digit = '1' + (row * 3 + col)
                    KeyButton(
                        label = digit.toString(),
                        enabled = enabled,
                        onClick = { onDigit(digit) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        ) {
            KeyButton(
                label = "0",
                enabled = enabled,
                onClick = { onDigit('0') },
                modifier = Modifier.weight(1f),
            )
            KeyButton(
                label = "",
                enabled = enabled,
                onClick = onBackspace,
                modifier = Modifier.weight(1f).semantics { testTag = "Backspace" },
            ) {
                Text(
                    text = "\u232B",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 24.sp,
                )
            }
            KeyButton(
                label = "",
                enabled = enabled && dotsCount > 0,
                onClick = onSubmit,
                modifier = Modifier.weight(1f).semantics { testTag = "Submit" },
            ) {
                SubmitButton()
            }
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (() -> Unit)? = null,
) {
    val bgColor = if (enabled) Color(0xFF2A2A2E) else Color(0xFF1C1C1E)
    val textColor = if (enabled) MaterialTheme.colorScheme.onSurface else Color(0xFF555555)

    Box(
        modifier = modifier
            .aspectRatio(1.5f)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (content != null) {
            content()
        } else {
            Text(
                text = label,
                color = textColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SubmitButton() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "\u2713",
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
