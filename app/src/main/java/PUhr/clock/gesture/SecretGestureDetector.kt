package PUhr.clock.gesture

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.awaitEachGesture
import androidx.compose.ui.input.pointer.awaitFirstDown
import androidx.compose.ui.input.pointer.awaitHorizontalDragOrCancellation
import androidx.compose.ui.input.pointer.awaitPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.waitForUpOrCancellation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

fun Modifier.secretGestureDetector(
    config: GestureConfig = GestureConfig(),
    onTrigger: () -> Unit,
): Modifier = this.pointerInput(config) {
    awaitEachGesture {
        val startNanos = System.nanoTime()

        // Step 1: 3 taps within tapWindowMs
        var firstTapNanos = 0L
        val tapsDone = withTimeoutOrNull(config.tapWindowMs) {
            repeat(3) { i ->
                val down = awaitFirstDown(requireUnconsumed = false)
                if (i == 0) firstTapNanos = System.nanoTime()
                if (waitForUpOrCancellation() == null) return@withTimeoutOrNull null
            }
            true
        } ?: return@awaitEachGesture

        // Step 2: swipe left (min swipeMinDp)
        val swipeMinPx = config.swipeMinDp.dp.toPx()
        val step2Timeout = config.totalTimeoutMs -
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)
        if (step2Timeout <= 0) return@awaitEachGesture

        val swipeOk = withTimeoutOrNull(step2Timeout) {
            val down = awaitFirstDown(requireUnconsumed = false)
            var total = 0f
            while (total > -swipeMinPx) {
                val delta = awaitHorizontalDragOrCancellation(down.id)
                    ?: return@withTimeoutOrNull false
                total += delta
            }
            true
        } ?: return@awaitEachGesture

        // Step 3: long press at lower longPressLowerFraction of container
        val lowerY = size.height * (1f - config.longPressLowerFraction)
        val step3Timeout = config.totalTimeoutMs -
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)
        if (step3Timeout <= 0) return@awaitEachGesture

        val down = withTimeoutOrNull(step3Timeout) {
            awaitFirstDown(requireUnconsumed = false)
        } ?: return@awaitEachGesture

        if (down.position.y < lowerY) return@awaitEachGesture

        val holdStartNanos = System.nanoTime()
        var held = false
        while (!held) {
            val holdElapsedMs = TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - holdStartNanos
            )
            val holdRemainingMs = config.longPressMs - holdElapsedMs
            val totalElapsedMs = TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startNanos
            )
            val totalRemainingMs = config.totalTimeoutMs - totalElapsedMs
            val waitMs = minOf(holdRemainingMs, totalRemainingMs)

            if (waitMs <= 0) {
                if (holdRemainingMs <= 0) held = true
                break
            }

            val event = withTimeoutOrNull(waitMs) {
                awaitPointerEvent(PointerEventPass.Main)
            }

            if (event == null) {
                if (holdRemainingMs <= 0) {
                    held = true
                    break
                }
                return@awaitEachGesture
            }

            val isDown = event.changes.any { it.id == down.id && it.pressed }
            if (!isDown) return@awaitEachGesture
        }

        onTrigger()
    }
}
