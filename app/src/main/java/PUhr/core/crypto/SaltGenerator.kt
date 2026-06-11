package PUhr.core.crypto

import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaltGenerator @Inject constructor() {
    private val random = SecureRandom()

    fun generate(): ByteArray {
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }
}
