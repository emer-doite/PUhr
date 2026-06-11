package PUhr.core.session

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class AutoLockTimerTest {
    private val timer = AutoLockTimer()

    @Test
    fun timerFires_onLockCallbackAfterDuration() = runBlocking {
        val fired = AtomicBoolean(false)

        timer.start(0) { fired.set(true) }
        kotlinx.coroutines.delay(200)

        assertTrue(fired.get())
    }

    @Test
    fun cancel_preventsOnLockFromBeingCalled() = runBlocking {
        val fired = AtomicBoolean(false)

        timer.start(2) { fired.set(true) }
        timer.cancel()
        kotlinx.coroutines.delay(100)

        assertFalse(fired.get())
    }
}
