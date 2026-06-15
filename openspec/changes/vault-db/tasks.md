# Tasks: vault-db

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~475 (8 new files, 1 modified, 4 test files) |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | ask-on-risk |
| Chain strategy | size-exception |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Medium

## Phase 1: Foundation

- [ ] 1.1 Update `core/session/SessionManager.kt` — add `dbk: ByteArray?` field, `getDbk(): ByteArray?`, `openSession(vek, dbk)` overload, zero-fill both on `closeSession()`
- [ ] 1.2 Create `data/db/VaultDatabaseProvider.kt` — synchronized open/close/getDatabase lifecycle, `SupportFactory(byte[])`, Room callback for PRAGMAs, WAL/SHM wipe via `SecureWipe.wipeFile()`
- [ ] 1.3 Create `di/DatabaseModule.kt` — Hilt `@Module` `@Provides VaultDatabaseProvider` (singleton), `@Binds VaultRepository` → `VaultRepositoryImpl`

## Phase 2: Data layer

- [ ] 2.1 Create `data/db/entity/VaultFileEntity.kt` — Room `@Entity` with 9 fields (id auto, fileName, mimeType, size, encryptedBlob, createdAt, modifiedAt, isFavorite, tags), `TagsConverter` for `List<String>`
- [ ] 2.2 Create `data/db/dao/VaultFileDao.kt` — `insert()` → Long, `getAll()` → `Flow<List<>>`, `getById()`, `delete()`, `search(query)` by `fileName LIKE`, `updateFavorite()`, `updateTags()`
- [ ] 2.3 Create `data/db/VaultDatabase.kt` — `@Database(entities=[VaultFileEntity], version=1)` with `VaultFileDao`, `fallbackToDestructiveMigration()`, Callback for cipher PRAGMAs

## Phase 3: Domain layer

- [ ] 3.1 Create `domain/repository/VaultRepository.kt` — interface mirroring DAO methods: `Flow<List<>>` for reads, suspend for writes
- [ ] 3.2 Create `data/repository/VaultRepositoryImpl.kt` — wraps `VaultDatabaseProvider`, delegates to DAO when open, returns empty-safe defaults when closed/null

## Phase 4: Tests

- [ ] 4.1 Create `VaultDatabaseProviderTest` — JVM: open/close lifecycle, double open idempotent, wrong key rejection, WAL/SHM cleanup via SecureWipe
- [ ] 4.2 Create `VaultFileDaoTest` — JVM in-memory SQLCipher: insert/getAll/getById/delete/search/updateFavorite/updateTags round-trips, tags converter
- [ ] 4.3 Create `VaultRepositoryTest` — JVM mock DAO: verify delegation when open, empty-safe defaults when closed
- [ ] 4.4 Update `SessionManagerTest` — add DBK storage, retrieval, dual zero-fill, null-after-close scenarios
