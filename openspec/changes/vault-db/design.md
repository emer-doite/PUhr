# Design: Vault Database (vault-db)

## Technical Approach

`VaultDatabaseProvider` singleton with SQLCipher `SupportFactory(byte[])` — Room's standard path for encrypted databases. Provider exposes `synchronized open(dbk)`, `close()`, `getDatabase()`. SessionManager extended to hold DBK alongside VEK; calls `open()` on unlock and `close()` on lock. Consumers inject Provider and null-check. DB filename disguised as `timely_cache.db`.

## Architecture Decisions

### Decision: VaultDatabaseProvider Singleton

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Hilt custom `@VaultScope` + component | Pure DI but complex Hilt wiring, no `@HiltViewModel` auto-wiring | ❌ |
| Singleton + synchronized mutable state | Mutable singleton but idiomatic Room + SQLCipher, zero boilerplate | ✅ |
| **Rationale**: Hilt custom components are over-engineered for MVP. The provider is a thin lifecycle holder — `synchronized` on `open`/`close`/`getDatabase` suffices. Can be refactored to scoped component later if justified. |

### Decision: DBK in SessionManager

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Hold DBK inline (in-memory) | More key material in memory; simple, matches VEK pattern | ✅ |
| Re-derive DBK on config change | Avoids extra key in memory; requires storing PIN longer | ❌ |
| **Rationale**: DBK is already returned from auth. SessionManager already holds VEK the same way. Both zero-filled on `closeSession()`. Re-derivation would extend PIN lifetime, which is worse. |

### Decision: VaultFileEntity Schema

Spec `REQ-VAULTDB-02` defines the entity schema. Deviates from `plam.md` (which uses `encryptedName`, `fileCategory`, `folderPath`, `iv`, `checksum`) toward MVP simplicity: `encryptedBlob` replaces per-field encryption, `tags` replaces `folderPath` + `fileCategory`.

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long (auto-generate) | Room-managed PK |
| `fileName` | String | Cleartext filename (MVP only; encrypted in production) |
| `mimeType` | String | e.g., "image/jpeg" |
| `size` | Long | File size in bytes |
| `encryptedBlob` | ByteArray | Full file content encrypted via AesGcmCipher |
| `createdAt` | Long | Epoch ms |
| `modifiedAt` | Long | Epoch ms |
| `isFavorite` | Boolean | Default false |
| `tags` | List\<String\> | TypeConverter joins/splits comma-separated |

### Decision: VaultRepository Empty-Safe Defaults

When `getDatabase()` returns null (closed/locked), `Flow<List<T>>` returns emptyList(), suspend functions return null or empty result. No exceptions propagated — callers don't need try/catch for locked state.

## Data Flow

```
PIN + Salt ──→ ArgonKeyDeriver ──→ 64B derived
                                        │
                              ┌─────────┴──────────┐
                              │                    │
                          [0..31] VEK         [32..63] DBK
                              │                    │
                              ▼                    ▼
                    SessionManager           SessionManager
                    .openSession(vek)        .dbk = dbk
                                                   │
                      AuthViewModel                 │
                      .uiState = Verified(dbk)      │
                                                   ▼
                      NavHost composable ──→ VaultDatabaseProvider.open(dbk)
                                                   │
                                                   ▼
                                           Room + SupportFactory
                                           ──→ timely_cache.db (encrypted)
                                                   │
                                                   ▼
                                           VaultRepositoryImpl
                                           ──→ VaultFileDao
                                                    │
                               ┌────────────────────┤
                               │                    │
                               ▼                    ▼
                        getAll(): Flow        insert/delete/update
                        search(query)         updateFavorite
                                              updateTags

On lock:
  SessionManager.closeSession()
    └── vaultKey?.fill(0), dbk?.fill(0), both = null
    └── VaultDatabaseProvider.close()
         ├── SupportFactory.setClearPassphrase(true)
         ├── database.close()
         ├── SecureWipe.wipeFile(timely_cache.db-wal)
         └── SecureWipe.wipeFile(timely_cache.db-shm)
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `data/db/entity/VaultFileEntity.kt` | Create | Room entity: 9 fields + TypeConverter for tags |
| `data/db/dao/VaultFileDao.kt` | Create | DAO: insert/getAll/getById/delete/search/updateFavorite/updateTags |
| `data/db/VaultDatabase.kt` | Create | Room @Database with entities, version=1, SupportFactory via callback |
| `data/db/VaultDatabaseProvider.kt` | Create | @Singleton: synchronized open/close/getDatabase |
| `data/repository/VaultRepositoryImpl.kt` | Create | Impl wrapping DAO, empty-safe on null provider |
| `domain/repository/VaultRepository.kt` | Create | Interface: mirror of DAO methods |
| `di/DatabaseModule.kt` | Create | Hilt @Module: @Provides VaultDatabaseProvider, @Binds VaultRepository |
| `core/session/SessionManager.kt` | Modify | Add `dbk` field, `getDbk()`, zero both on close, `openSession(vek, dbk)` overload |

## Interfaces / Contracts

```kotlin
// VaultDatabaseProvider
@Singleton
class VaultDatabaseProvider @Inject constructor(
    private val context: Context
) {
    @Synchronized fun open(dbk: ByteArray): VaultDatabase
    @Synchronized fun close()
    @Synchronized fun getDatabase(): VaultDatabase?
}

// VaultRepository
interface VaultRepository {
    suspend fun insert(entity: VaultFileEntity): Long?
    fun getAll(): Flow<List<VaultFileEntity>>
    suspend fun getById(id: Long): VaultFileEntity?
    suspend fun delete(entity: VaultFileEntity)
    fun search(query: String): Flow<List<VaultFileEntity>>
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)
    suspend fun updateTags(id: Long, tags: List<String>)
}

// SessionManager additions
fun openSession(vek: ByteArray, dbk: ByteArray)
fun getDbk(): ByteArray?
```

## Testing Strategy

| Layer | What | Approach |
|-------|------|----------|
| Unit (JVM) | SessionManager | Existing tests updated: openSession(vek, dbk), getDbk(), dual zero-fill |
| Unit (JVM) | VaultFileDao | In-memory SupportFactory + `:memory:` SQLCipher DB — verify insert/query/search/favorite round-trips |
| Unit (JVM) | VaultFileEntity | Tags TypeConverter: "work,important" ↔ List, null/empty handling |
| Unit (JVM) | VaultRepository | Mock provider; verify empty-safe when null, delegation when open |
| Unit (JVM) | VaultDatabaseProvider | Synchronization test: open/close/getDatabase sequence, double open, WAL/SHM wipe via SecureWipe |
| Compile | DatabaseModule | `./gradlew assembleDebug` resolves all DI bindings |

## Migration / Rollout

`fallbackToDestructiveMigration()` on Room database builder for MVP. No data migration needed — DB starts empty. Pre-release migrations added later.

## Open Questions

- [ ] `SupportFactory` with `:memory:` database for unit tests — does SQLCipher 4.5.4 support this? If not, tests need temporary file DB.
- [ ] `byte[]` vs `char[]` for DBK to `SupportFactory` — spec uses `byte[]` for now; SQLCipher recommends `char[]` for memory control. Accept tradeoff.
- [ ] When does `onVerified(dbk)` trigger in NavHost? Need to confirm the composing component passes DBK to SessionManager and calls `VaultDatabaseProvider.open()`.
