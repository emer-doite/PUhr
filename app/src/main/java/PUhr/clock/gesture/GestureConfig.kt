package PUhr.clock.gesture

data class GestureConfig(
    val tapSequenceTimeoutMs: Long = 2000L,
    val tapToleranceDp: Float = 30f,
    val tapMaxIntervalMs: Long = 300L,
    val swipeMinLengthDp: Float = 120f,
    val swipeTimeoutMs: Long = 1500L,
    val longPressHoldMs: Long = 800L,
    val longPressLowerFraction: Float = 0.3f,
    val totalTimeoutMs: Long = 8000L,
)
