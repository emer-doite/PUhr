package PUhr.core.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class KeyDeriverContractTest {
    private val deriver = FakeKeyDeriver()

    @Test
    fun samePinAndSalt_producesSameOutput() {
        val pin = "1234"
        val salt = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)

        val result1 = deriver.derive(pin, salt)
        val result2 = deriver.derive(pin, salt)

        assertArrayEquals(result1, result2)
    }

    @Test
    fun differentSalts_produceDifferentOutputs() {
        val pin = "1234"
        val salt1 = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        val salt2 = byteArrayOf(16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1)

        val result1 = deriver.derive(pin, salt1)
        val result2 = deriver.derive(pin, salt2)

        assertFalse(result1.contentEquals(result2))
    }
}
