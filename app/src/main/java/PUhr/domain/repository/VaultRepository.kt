package PUhr.domain.repository

import PUhr.data.db.entity.VaultFileEntity
import kotlinx.coroutines.flow.Flow

interface VaultRepository {

    suspend fun insert(entity: VaultFileEntity): Long?

    fun getAll(): Flow<List<VaultFileEntity>>

    suspend fun getById(id: Long): VaultFileEntity?

    suspend fun delete(entity: VaultFileEntity)

    fun search(query: String): Flow<List<VaultFileEntity>>

    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    suspend fun updateTags(id: Long, tags: List<String>)
}
