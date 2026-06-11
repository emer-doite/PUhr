package PUhr.core.crypto

import java.security.MessageDigest

class FakeKeyDeriver : KeyDeriver {
    override fun derive(pin: String, salt: ByteArray): ByteArray {
        val input = pin + salt.joinToString("") { "%02x".format(it) }
        return MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    }
}
