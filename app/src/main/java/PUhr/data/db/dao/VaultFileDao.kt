package PUhr.data.db.dao

import PUhr.data.db.entity.VaultFileEntity
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultFileDao {

    @Insert
    suspend fun insert(entity: VaultFileEntity): Long

    @Query("SELECT * FROM vault_files ORDER BY createdAt DESC")
    fun getAll(): Flow<List<VaultFileEntity>>

    @Query("SELECT * FROM vault_files WHERE id = :id")
    suspend fun getById(id: Long): VaultFileEntity?

    @Delete
    suspend fun delete(entity: VaultFileEntity)

    @Query("SELECT * FROM vault_files WHERE fileName LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun search(query: String): Flow<List<VaultFileEntity>>

    @Query("UPDATE vault_files SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE vault_files SET tags = :tags WHERE id = :id")
    suspend fun updateTags(id: Long, tags: List<String>)
}
