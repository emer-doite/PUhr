package PUhr.core.session

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {

    @Volatile
    private var vaultKey: ByteArray? = null

    fun openSession(key: ByteArray) {
        vaultKey = key.copyOf()
    }

    fun closeSession() {
        vaultKey?.fill(0)
        vaultKey = null
    }

    fun getVaultKey(): ByteArray? = vaultKey?.copyOf()

    fun isActive(): Boolean = vaultKey != null
}
