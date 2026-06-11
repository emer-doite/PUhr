package PUhr.core.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class SaltGeneratorTest {
    private val generator = SaltGenerator()

    @Test
    fun generatedSaltIs16Bytes() {
        val salt = generator.generate()
        assertEquals(16, salt.size)
    }

    @Test
    fun twoSuccessiveCalls_produceDifferentSalts() {
        val salt1 = generator.generate()
        val salt2 = generator.generate()
        assertFalse(salt1.contentEquals(salt2))
    }

    @Test
    fun saltIsNotAllZeros() {
        val salt = generator.generate()
        assertFalse(salt.all { it == 0.toByte() })
    }
}
