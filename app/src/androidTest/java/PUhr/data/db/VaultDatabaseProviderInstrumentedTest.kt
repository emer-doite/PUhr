package PUhr.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.sqlcipher.database.SupportFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.SecureRandom

@RunWith(AndroidJUnit4::class)
class VaultDatabaseProviderInstrumentedTest {

    private lateinit var provider: VaultDatabaseProvider

    private val testDbk = ByteArray(32).also { SecureRandom().nextBytes(it) }

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        provider = VaultDatabaseProvider(context)
    }

    @After
    fun tearDown() {
        provider.close()
    }

    @Test
    fun getDatabase_returnsNullBeforeOpen() {
        assertNull(provider.getDatabase())
    }

    @Test
    fun open_returnsDatabase() {
        val dbk = testDbk
        val db = provider.open(dbk)
        assertNotNull(db)
        assertEquals(db, provider.getDatabase())
    }

    @Test
    fun close_clearsDatabase() {
        provider.open(testDbk)
        provider.close()
        assertNull(provider.getDatabase())
    }

    @Test
    fun doubleOpen_returnsSameInstance() {
        val db1 = provider.open(testDbk)
        val db2 = provider.open(testDbk)
        assertEquals(db1, db2)
    }

    @Test
    fun close_onUnopenedProvider_doesNotThrow() {
        provider.close()
    }
}
