package PUhr.data.db

import PUhr.data.db.dao.VaultFileDao
import PUhr.data.db.entity.VaultFileEntity
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.sqlcipher.database.SupportFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.SecureRandom

@RunWith(AndroidJUnit4::class)
class VaultFileDaoIntegrationTest {

    private lateinit var database: VaultDatabase
    private lateinit var dao: VaultFileDao

    @Before
    fun setUp() {
        val passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val factory = SupportFactory(passphrase)
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VaultDatabase::class.java,
        )
            .openHelperFactory(factory)
            .allowMainThreadQueries()
            .build()
        dao = database.vaultFileDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insert_autoGeneratesId() = runTest {
        val entity = VaultFileEntity(
            fileName = "test.txt",
            mimeType = "text/plain",
            size = 100L,
            encryptedBlob = byteArrayOf(1, 2, 3),
            createdAt = 1000L,
            modifiedAt = 2000L,
        )
        val id = dao.insert(entity)
        assertTrue(id > 0)
    }

    @Test
    fun insertAndGetById() = runTest {
        val entity = VaultFileEntity(
            fileName = "photo.jpg",
            mimeType = "image/jpeg",
            size = 2048L,
            encryptedBlob = byteArrayOf(0xDE.toByte(), 0xAD.toByte()),
            createdAt = 5000L,
            modifiedAt = 6000L,
        )
        val id = dao.insert(entity)
        val retrieved = dao.getById(id)
        assertNotNull(retrieved)
        assertEquals("photo.jpg", retrieved!!.fileName)
        assertTrue(retrieved.encryptedBlob.contentEquals(byteArrayOf(0xDE.toByte(), 0xAD.toByte())))
    }

    @Test
    fun getAll_orderedByCreatedAtDesc() = runTest {
        dao.insert(VaultFileEntity(fileName = "old.txt", mimeType = "text", size = 1L, encryptedBlob = ByteArray(0), createdAt = 100L, modifiedAt = 100L))
        dao.insert(VaultFileEntity(fileName = "new.txt", mimeType = "text", size = 2L, encryptedBlob = ByteArray(0), createdAt = 300L, modifiedAt = 300L))
        dao.insert(VaultFileEntity(fileName = "mid.txt", mimeType = "text", size = 3L, encryptedBlob = ByteArray(0), createdAt = 200L, modifiedAt = 200L))

        val all = dao.getAll().first()
        assertEquals(3, all.size)
        assertEquals("new.txt", all[0].fileName)
        assertEquals("mid.txt", all[1].fileName)
        assertEquals("old.txt", all[2].fileName)
    }

    @Test
    fun delete_removesEntity() = runTest {
        val entity = VaultFileEntity(fileName = "del.txt", mimeType = "text", size = 1L, encryptedBlob = ByteArray(0), createdAt = 100L, modifiedAt = 100L)
        val id = dao.insert(entity)
        dao.delete(entity.copy(id = id))
        assertNull(dao.getById(id))
    }

    @Test
    fun searchByFileName() = runTest {
        dao.insert(VaultFileEntity(fileName = "photo.jpg", mimeType = "image", size = 1L, encryptedBlob = ByteArray(0), createdAt = 100L, modifiedAt = 100L))
        dao.insert(VaultFileEntity(fileName = "note.txt", mimeType = "text", size = 1L, encryptedBlob = ByteArray(0), createdAt = 200L, modifiedAt = 200L))

        val results = dao.search("photo").first()
        assertEquals(1, results.size)
        assertEquals("photo.jpg", results[0].fileName)
    }

    @Test
    fun updateFavorite() = runTest {
        val id = dao.insert(VaultFileEntity(fileName = "fav.txt", mimeType = "text", size = 1L, encryptedBlob = ByteArray(0), createdAt = 100L, modifiedAt = 100L))
        dao.updateFavorite(id, true)
        assertTrue(dao.getById(id)!!.isFavorite)
    }

    @Test
    fun updateTags() = runTest {
        val id = dao.insert(VaultFileEntity(fileName = "tags.txt", mimeType = "text", size = 1L, encryptedBlob = ByteArray(0), createdAt = 100L, modifiedAt = 100L, tags = listOf("a")))
        dao.updateTags(id, listOf("x", "y"))
        assertEquals(listOf("x", "y"), dao.getById(id)!!.tags)
    }

    @Test
    fun getById_returnsNullForNonExistent() = runTest {
        assertNull(dao.getById(999L))
    }
}
