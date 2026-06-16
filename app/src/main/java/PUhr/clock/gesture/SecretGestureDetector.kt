package PUhr.clock.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withTimeoutOrNull

fun Modifier.secretGestureDetector(
    config: GestureConfig = GestureConfig(),
    onTrigger: () -> Unit,
): Modifier = this.pointerInput(config) {
    val swipeMinPx = config.swipeMinDp.dp.toPx()
    awaitEachGesture {
        val startNanos = System.nanoTime()

        // Step 1: 3 taps within tapWindowMs
        val tapResult = withTimeoutOrNull(config.tapWindowMs) {
            repeat(3) {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (waitForUpOrCancellation() == null) return@withTimeoutOrNull false
            }
            true
        }
        if (tapResult != true) return@awaitEachGesture

        // Step 2: swipe left (min swipeMinDp)
        val elapsedMs1 = nanosToMs(System.nanoTime() - startNanos)
        val step2Timeout = config.totalTimeoutMs - elapsedMs1
        if (step2Timeout <= 0) return@awaitEachGesture

        val swipeResult = withTimeoutOrNull(step2Timeout) {
            val down = awaitFirstDown(requireUnconsumed = false)
            var totalDeltaX = 0f
            var pointerId = down.id
            var cancelled = false

            while (!cancelled && totalDeltaX > -swipeMinPx) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val change = event.changes.firstOrNull { it.id == pointerId }
                if (change == null || !change.pressed) {
                    cancelled = true
                } else {
                    totalDeltaX += (change.position.x - change.previousPosition.x)
                    change.consume()
                }
            }
            !cancelled && totalDeltaX <= -swipeMinPx
        }
        if (swipeResult != true) return@awaitEachGesture

        // Step 3: long press at lower longPressLowerFraction of container
        val elapsedMs2 = nanosToMs(System.nanoTime() - startNanos)
        val step3Timeout = config.totalTimeoutMs - elapsedMs2
        if (step3Timeout <= 0) return@awaitEachGesture

        val lowerY = size.height * (1f - config.longPressLowerFraction)

        val downResult = withTimeoutOrNull(step3Timeout) {
            awaitFirstDown(requireUnconsumed = false)
        }
        if (downResult == null) return@awaitEachGesture

        // Check if press is in lower zone
        if (downResult.position.y < lowerY) return@awaitEachGesture

        // Hold for longPressMs — poll pointer events
        val holdStartNanos = System.nanoTime()
        var heldLongEnough = false
        var pointerReleased = false

        while (!heldLongEnough && !pointerReleased) {
            val holdElapsedMs = nanosToMs(System.nanoTime() - holdStartNanos)
            val totalElapsedMs = nanosToMs(System.nanoTime() - startNanos)
            val holdRemainingMs = config.longPressMs - holdElapsedMs
            val totalRemainingMs = config.totalTimeoutMs - totalElapsedMs
            val waitMs = minOf(holdRemainingMs, totalRemainingMs)

            if (waitMs <= 0) {
                heldLongEnough = holdRemainingMs <= 0
                break
            }

            val event = withTimeoutOrNull(waitMs) {
                awaitPointerEvent(PointerEventPass.Main)
            }

            if (event == null) {
                heldLongEnough = nanosToMs(System.nanoTime() - holdStartNanos) >= config.longPressMs
                break
            }

            val stillDown = event.changes.any { change ->
                change.id == downResult.id && change.pressed
            }
            if (!stillDown) {
                pointerReleased = true
            }
        }

        if (heldLongEnough) {
            onTrigger()
        }
    }
}

private fun nanosToMs(nanos: Long): Long = nanos / 1_000_000
