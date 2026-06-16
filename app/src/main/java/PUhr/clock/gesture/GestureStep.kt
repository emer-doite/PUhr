package PUhr.clock.gesture

sealed class GestureStep {

    data object Idle : GestureStep()
    data class ThreeTaps(val swipeDeadline: Long) : GestureStep()
    data class Swiped(val longPressDeadline: Long) : GestureStep()
    data object LongPressed : GestureStep()
    data object Triggered : GestureStep()
    data class TimedOut(val failedStep: String) : GestureStep()

    fun next(event: GestureEvent, config: GestureConfig): GestureStep = when (this) {
        is Idle -> when (event) {
            is GestureEvent.TapsCompleted -> ThreeTaps(event.swipeDeadline)
            is GestureEvent.Timeout -> this
            else -> this
        }
        is ThreeTaps -> when (event) {
            is GestureEvent.SwipeCompleted -> Swiped(event.longPressDeadline)
            is GestureEvent.Timeout -> TimedOut("ThreeTaps")
            else -> Idle
        }
        is Swiped -> when (event) {
            is GestureEvent.LongPressCompleted -> LongPressed
            is GestureEvent.Timeout -> TimedOut("Swiped")
            else -> Idle
        }
        is LongPressed -> when (event) {
            is GestureEvent.Release -> Triggered
            is GestureEvent.Timeout -> TimedOut("LongPressed")
            else -> Idle
        }
        is Triggered -> this
        is TimedOut -> Idle
    }
}

sealed class GestureEvent {
    data class TapsCompleted(val swipeDeadline: Long) : GestureEvent()
    data class SwipeCompleted(val longPressDeadline: Long) : GestureEvent()
    data object LongPressCompleted : GestureEvent()
    data object Release : GestureEvent()
    data object Timeout : GestureEvent()
}
