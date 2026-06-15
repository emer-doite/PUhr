package PUhr.core.session

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {
    private val manager = SessionManager()
    private val vaultKey = ByteArray(32).also { it[0] = 0xAB.toByte(); it[31] = 0xCD.toByte() }
    private val dbk = ByteArray(32).also { it[0] = 0x01.toByte(); it[31] = 0x02.toByte() }

    @Test
    fun openSession_setsActiveState() {
        manager.openSession(vaultKey)
        assertTrue(manager.isActive())
        manager.closeSession()
    }

    @Test
    fun getVaultKey_returnsKeyAfterOpenSession() {
        manager.openSession(vaultKey)
        val retrieved = manager.getVaultKey()
        assertArrayEquals(vaultKey, retrieved)
        manager.closeSession()
    }

    @Test
    fun closeSession_clearsKeyAndZeroesMemory() {
        manager.openSession(vaultKey)
        manager.closeSession()
        assertNull(manager.getVaultKey())
        assertFalse(manager.isActive())
    }

    @Test
    fun closeSession_withNoActiveSession_isSafe() {
        manager.closeSession()
    }

    @Test
    fun isActive_reflectsSessionState() {
        assertFalse(manager.isActive())
        manager.openSession(vaultKey)
        assertTrue(manager.isActive())
        manager.closeSession()
        assertFalse(manager.isActive())
    }

    @Test
    fun openSessionWithDbk_storesBothKeys() {
        manager.openSession(vaultKey, dbk)
        assertTrue(manager.isActive())
        assertArrayEquals(vaultKey, manager.getVaultKey())
        assertArrayEquals(dbk, manager.getDbk())
        manager.closeSession()
    }

    @Test
    fun getDbk_returnsDbkAfterOpenSession() {
        manager.openSession(vaultKey, dbk)
        val retrieved = manager.getDbk()
        assertArrayEquals(dbk, retrieved)
        manager.closeSession()
    }

    @Test
    fun getDbk_returnsNullWhenNotOpened() {
        assertNull(manager.getDbk())
    }

    @Test
    fun getDbk_returnsNullAfterCloseSession() {
        manager.openSession(vaultKey, dbk)
        manager.closeSession()
        assertNull(manager.getDbk())
    }

    @Test
    fun closeSession_zeroesBothVekAndDbk() {
        val vekCopy = vaultKey.copyOf()
        val dbkCopy = dbk.copyOf()
        manager.openSession(vekCopy, dbkCopy)
        manager.closeSession()
        assertNull(manager.getVaultKey())
        assertNull(manager.getDbk())
    }

    @Test
    fun openSessionWithoutDbk_setsDbkToNull() {
        manager.openSession(vaultKey, dbk)
        manager.openSession(vaultKey)
        assertNull(manager.getDbk())
        manager.closeSession()
    }
}
