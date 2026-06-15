package PUhr.data.repository

import PUhr.data.db.VaultDatabaseProvider
import PUhr.data.db.dao.VaultFileDao
import PUhr.data.db.entity.VaultFileEntity
import PUhr.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepositoryImpl @Inject constructor(
    private val provider: VaultDatabaseProvider,
) : VaultRepository {

    private val dao: VaultFileDao?
        get() = provider.getDatabase()?.vaultFileDao()

    override suspend fun insert(entity: VaultFileEntity): Long? =
        dao?.insert(entity)

    override fun getAll(): Flow<List<VaultFileEntity>> =
        dao?.getAll() ?: emptyFlow()

    override suspend fun getById(id: Long): VaultFileEntity? =
        dao?.getById(id)

    override suspend fun delete(entity: VaultFileEntity) {
        dao?.delete(entity)
    }

    override fun search(query: String): Flow<List<VaultFileEntity>> =
        dao?.search(query) ?: emptyFlow()

    override suspend fun updateFavorite(id: Long, isFavorite: Boolean) {
        dao?.updateFavorite(id, isFavorite)
    }

    override suspend fun updateTags(id: Long, tags: List<String>) {
        dao?.updateTags(id, tags)
    }
}
