package PUhr.core.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AesGcmCipher @Inject constructor() {
    companion object {
        const val ALGORITHM = "AES/GCM/NoPadding"
        const val KEY_SIZE_BITS = 256
        const val IV_SIZE_BYTES = 12
        const val TAG_SIZE_BITS = 128
        val HEADER_MAGIC = byteArrayOf(0x50, 0x56, 0x45, 0x4E) // "PVEN"
    }

    fun encrypt(plaintext: ByteArray, key: ByteArray): EncryptedData {
        require(key.size == KEY_SIZE_BITS / 8) { "Key must be 256 bits" }
        val iv = ByteArray(IV_SIZE_BYTES).also { SecureRandom.getInstanceStrong().nextBytes(it) }
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_SIZE_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)
        return EncryptedData(header = HEADER_MAGIC, iv = iv, ciphertext = ciphertext)
    }

    fun decrypt(iv: ByteArray, ciphertext: ByteArray, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_SIZE_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    fun decrypt(encryptedBlob: ByteArray, key: ByteArray): ByteArray {
        require(encryptedBlob.size >= 4 + IV_SIZE_BYTES + 1) { "Blob too short" }
        require(encryptedBlob.sliceArray(0 until 4).contentEquals(HEADER_MAGIC)) { "Invalid header" }
        val iv = encryptedBlob.sliceArray(4 until 4 + IV_SIZE_BYTES)
        val ciphertext = encryptedBlob.sliceArray(4 + IV_SIZE_BYTES until encryptedBlob.size)
        return decrypt(iv, ciphertext, key)
    }
}

data class EncryptedData(
    val header: ByteArray,
    val iv: ByteArray,
    val ciphertext: ByteArray
) {
    fun toBlob(): ByteArray = header + iv + ciphertext
}
