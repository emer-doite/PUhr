package PUhr.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_files")
data class VaultFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val encryptedBlob: ByteArray,
    val createdAt: Long,
    val modifiedAt: Long,
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
)
