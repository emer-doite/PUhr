package PUhr.clock.gesture

data class GestureConfig(
    val tapWindowMs: Long = 2000L,
    val swipeMinDp: Float = 120f,
    val longPressMs: Long = 800L,
    val longPressLowerFraction: Float = 0.3f,
    val totalTimeoutMs: Long = 8000L,
)
