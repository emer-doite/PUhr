package PUhr.data.db

import PUhr.data.db.entity.TagsConverter
import PUhr.data.db.entity.VaultFileEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultFileDaoTest {

    // TagsConverter is testable in pure JVM
    @Test
    fun tagsConverter_fromTags_joinsWithComma() {
        val converter = TagsConverter()
        val result = converter.fromTags(listOf("work", "important"))
        assertEquals("work,important", result)
    }

    @Test
    fun tagsConverter_toTags_splitsOnComma() {
        val converter = TagsConverter()
        val result = converter.toTags("work,important")
        assertEquals(listOf("work", "important"), result)
    }

    @Test
    fun tagsConverter_handlesEmpty() {
        val converter = TagsConverter()
        val result = converter.toTags("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun tagsConverter_handlesBlank() {
        val converter = TagsConverter()
        val result = converter.toTags("   ")
        assertTrue(result.isEmpty())
    }

    @Test
    fun tagsConverter_roundTrip() {
        val converter = TagsConverter()
        val tags = listOf("personal", "finance", "2026")
        val encoded = converter.fromTags(tags)
        val decoded = converter.toTags(encoded)
        assertEquals(tags, decoded)
    }

    @Test
    fun entity_defaultValues() {
        val entity = VaultFileEntity(
            fileName = "test.txt",
            mimeType = "text/plain",
            size = 100L,
            encryptedBlob = byteArrayOf(1, 2, 3),
            createdAt = 1000L,
            modifiedAt = 2000L,
        )
        assertEquals(0L, entity.id)
        assertEquals(false, entity.isFavorite)
        assertEquals(emptyList<String>(), entity.tags)
    }
}
