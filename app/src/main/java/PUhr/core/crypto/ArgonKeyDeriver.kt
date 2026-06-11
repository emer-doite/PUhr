package PUhr.core.crypto

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import javax.inject.Inject

class ArgonKeyDeriver @Inject constructor() : KeyDeriver {
    companion object {
        private const val MEMORY_KB = 65536
        private const val ITERATIONS = 3
        private const val PARALLELISM = 4
        private const val KEY_LENGTH_BYTES = 32
    }

    override fun derive(pin: String, salt: ByteArray): ByteArray {
        val argon2 = Argon2Kt()
        val result = argon2.hash(
            mode = Argon2Mode.ARGON2_ID,
            password = pin.toByteArray(Charsets.UTF_8),
            salt = salt,
            tCostInIterations = ITERATIONS,
            mCostInKibibyte = MEMORY_KB,
            parallelism = PARALLELISM,
            hashLengthInBytes = KEY_LENGTH_BYTES,
        )
        return result.rawHashAsByteArray()
    }
}
