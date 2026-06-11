package PUhr.clock

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class TimeFormatterTest {

    @Test
    fun formatHMS_returnsCorrectFormat() {
        assertEquals("14:30:45", LocalTime.of(14, 30, 45).formatHMS())
    }

    @Test
    fun formatHMS_midnight_returnsZeroPadded() {
        assertEquals("00:00:00", LocalTime.of(0, 0, 0).formatHMS())
    }

    @Test
    fun formatHMS_justBeforeMidnight_returnsCorrect() {
        assertEquals("23:59:59", LocalTime.of(23, 59, 59).formatHMS())
    }
}
