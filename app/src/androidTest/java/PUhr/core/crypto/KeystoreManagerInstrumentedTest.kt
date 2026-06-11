package PUhr.core.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.SecureRandom

@RunWith(AndroidJUnit4::class)
class KeystoreManagerInstrumentedTest {
    private val keystoreManager = KeystoreManager()

    @After
    fun tearDown() {
        keystoreManager.deleteKey()
    }

    @Test
    fun generateWrappingKey_createsKeyInKeystore() {
        keystoreManager.generateWrappingKey()
        assertTrue(keystoreManager.hasKey())
    }

    @Test
    fun wrapKey_unwrapKey_roundTrip_returnsOriginalKey() {
        keystoreManager.generateWrappingKey()
        val originalKey = ByteArray(32).also { SecureRandom().nextBytes(it) }

        val wrapped = keystoreManager.wrapKey(originalKey)
        val unwrapped = keystoreManager.unwrapKey(wrapped)

        assertArrayEquals(originalKey, unwrapped)
    }

    @Test
    fun deleteKey_removesKeyFromKeystore() {
        keystoreManager.generateWrappingKey()
        assertTrue(keystoreManager.hasKey())

        keystoreManager.deleteKey()
        assertFalse(keystoreManager.hasKey())
    }

    @Test
    fun hasKey_returnsCorrectState() {
        assertFalse(keystoreManager.hasKey())

        keystoreManager.generateWrappingKey()
        assertTrue(keystoreManager.hasKey())

        keystoreManager.deleteKey()
        assertFalse(keystoreManager.hasKey())
    }
}
