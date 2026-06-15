package PUhr.core.session

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {

    @Volatile
    private var vaultKey: ByteArray? = null

    @Volatile
    private var dbk: ByteArray? = null

    fun openSession(key: ByteArray) {
        vaultKey = key.copyOf()
        dbk = null
    }

    fun openSession(vek: ByteArray, dbk: ByteArray) {
        vaultKey = vek.copyOf()
        this.dbk = dbk.copyOf()
    }

    fun closeSession() {
        vaultKey?.fill(0)
        vaultKey = null
        dbk?.fill(0)
        dbk = null
    }

    fun getVaultKey(): ByteArray? = vaultKey?.copyOf()

    fun getDbk(): ByteArray? = dbk?.copyOf()

    fun isActive(): Boolean = vaultKey != null
}
