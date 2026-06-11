package PUhr.core.session

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {
    private val manager = SessionManager()
    private val vaultKey = ByteArray(32).also { it[0] = 0xAB.toByte(); it[31] = 0xCD.toByte() }

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
}
