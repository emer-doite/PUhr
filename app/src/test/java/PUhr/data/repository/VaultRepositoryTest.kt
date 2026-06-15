package PUhr.data.repository

import PUhr.data.db.VaultDatabase
import PUhr.data.db.VaultDatabaseProvider
import PUhr.data.db.dao.VaultFileDao
import PUhr.data.db.entity.VaultFileEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class VaultRepositoryTest {

    private val mockProvider = mockk<VaultDatabaseProvider>()
    private val mockDao = mockk<VaultFileDao>()
    private lateinit var repository: VaultRepositoryImpl

    @Before
    fun setUp() {
        repository = VaultRepositoryImpl(mockProvider)
    }

    @Test
    fun getAll_returnsEmptyFlowWhenProviderIsClosed() = runTest {
        every { mockProvider.getDatabase() } returns null
        val result = repository.getAll().firstOrNull()
        assertNull(result)
    }

    @Test
    fun insert_returnsNullWhenProviderIsClosed() = runTest {
        every { mockProvider.getDatabase() } returns null
        val entity = VaultFileEntity(
            fileName = "test.txt",
            mimeType = "text/plain",
            size = 100L,
            encryptedBlob = ByteArray(0),
            createdAt = 0L,
            modifiedAt = 0L,
        )
        val result = repository.insert(entity)
        assertNull(result)
    }

    @Test
    fun getById_returnsNullWhenProviderIsClosed() = runTest {
        every { mockProvider.getDatabase() } returns null
        val result = repository.getById(1L)
        assertNull(result)
    }

    @Test
    fun delegateToDao_whenProviderIsOpen() = runTest {
        val mockDb = mockk<VaultDatabase>()
        val entity = VaultFileEntity(
            fileName = "photo.jpg",
            mimeType = "image/jpeg",
            size = 1024L,
            encryptedBlob = ByteArray(0),
            createdAt = 1000L,
            modifiedAt = 1000L,
        )

        every { mockProvider.getDatabase() } returns mockDb
        every { mockDb.vaultFileDao() } returns mockDao
        coEvery { mockDao.insert(entity) } returns 1L
        every { mockDao.getAll() } returns flowOf(listOf(entity))

        val id = repository.insert(entity)
        assertNotNull(id)
        assertEquals(1L, id)

        val flow = repository.getAll()
        assertNotNull(flow)

        coVerify { mockDao.insert(entity) }
    }

    @Test
    fun updateTags_convertsListToString() = runTest {
        val mockDb = mockk<VaultDatabase>()
        every { mockProvider.getDatabase() } returns mockDb
        every { mockDb.vaultFileDao() } returns mockDao
        coEvery { mockDao.updateTags(1L, any<List<String>>()) } returns Unit

        repository.updateTags(1L, listOf("work", "important"))

        coVerify { mockDao.updateTags(1L, listOf("work", "important")) }
    }
}
