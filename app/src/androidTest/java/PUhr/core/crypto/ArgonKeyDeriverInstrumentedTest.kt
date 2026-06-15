package PUhr.core.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArgonKeyDeriverInstrumentedTest {
    private val deriver = ArgonKeyDeriver()

    @Test
    fun samePinAndSalt_producesSame64ByteOutput() {
        val pin = "1234"
        val salt = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)

        val result1 = deriver.derive(pin, salt)
        val result2 = deriver.derive(pin, salt)

        assertEquals(64, result1.size)
        assertArrayEquals(result1, result2)
    }

    @Test
    fun differentSalts_produceDifferentOutputs() {
        val pin = "1234"
        val salt1 = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        val salt2 = byteArrayOf(16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1)

        val result1 = deriver.derive(pin, salt1)
        val result2 = deriver.derive(pin, salt2)

        assertEquals(64, result1.size)
        assertEquals(64, result2.size)
        assertFalse(result1.contentEquals(result2))
    }

    @Test
    fun outputSplitsIntoVekAndDbk() {
        val pin = "1234"
        val salt = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)

        val result = deriver.derive(pin, salt)

        val vek = result.copyOfRange(0, 32)
        val dbk = result.copyOfRange(32, 64)
        assertEquals(32, vek.size)
        assertEquals(32, dbk.size)
        assertFalse(vek.contentEquals(dbk))
    }
}
