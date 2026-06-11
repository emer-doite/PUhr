package PUhr.core.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.security.SecureRandom
import javax.crypto.AEADBadTagException

class AesGcmCipherTest {
    private val cipher = AesGcmCipher()
    private val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
    private val plaintext = "Hello PhantomVault".toByteArray()

    @Test
    fun encryptDecryptRoundTrip() {
        val encrypted = cipher.encrypt(plaintext, key)
        val decrypted = cipher.decrypt(encrypted.toBlob(), key)
        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun tamperedCiphertext_throwsAEADBadTagException() {
        val encrypted = cipher.encrypt(plaintext, key)
        val blob = encrypted.toBlob()
        blob[blob.lastIndex] = (blob.last().toInt() xor 0xFF).toByte()

        assertThrows(AEADBadTagException::class.java) {
            cipher.decrypt(blob, key)
        }
    }

    @Test
    fun ivIsUniqueAcrossEncryptions() {
        val encrypted1 = cipher.encrypt(plaintext, key)
        val encrypted2 = cipher.encrypt(plaintext, key)
        assertNotEquals(encrypted1.iv.toList(), encrypted2.iv.toList())
    }

    @Test
    fun decryptWithWrongKey_throwsAEADBadTagException() {
        val encrypted = cipher.encrypt(plaintext, key)
        val wrongKey = ByteArray(32).also { SecureRandom().nextBytes(it) }

        assertThrows(AEADBadTagException::class.java) {
            cipher.decrypt(encrypted.toBlob(), wrongKey)
        }
    }
}
