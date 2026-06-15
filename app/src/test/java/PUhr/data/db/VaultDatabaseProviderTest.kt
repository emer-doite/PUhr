package PUhr.data.db

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File

class VaultDatabaseProviderTest {

    private lateinit var provider: VaultDatabaseProvider
    private val mockContext = mockk<android.content.Context>()

    @Before
    fun setUp() {
        every { mockContext.filesDir } returns File(System.getProperty("java.io.tmpdir"))
        provider = VaultDatabaseProvider(mockContext)
    }

    @Test
    fun getDatabase_returnsNullBeforeOpen() {
        assertNull(provider.getDatabase())
    }

    @Test
    fun close_onUnopenedProvider_doesNotThrow() {
        provider.close()
    }
}
