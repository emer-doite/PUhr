package PUhr.core.crypto

interface KeyDeriver {
    fun derive(pin: String, salt: ByteArray): ByteArray
}
