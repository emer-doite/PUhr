# Proposal: Vault Database (vault-db)

## Intent

Create the encrypted SQLCipher-backed Room database for vault file metadata. Auth layer already derives DBK and returns it in `UnlockResult.Success` — but nothing consumes it. This change wires DBK into a lifecycle-aware `VaultDatabaseProvider`, defines `VaultFileEntity` + DAO, provides `VaultRepository`, and registers Hilt wiring.

## Scope

### In Scope
- `VaultDatabase` with SQLCipher via `SupportFactory` (Room + `net.zetetic:android-database-sqlcipher:4.5.4`)
- `VaultFileEntity` + `VaultFileDao` (CRUD, queries by category/folder/date)
- `VaultDatabaseProvider` singleton — `open(dbk)`, `close()`, `getDatabase(): VaultDatabase?`
- `VaultRepository` interface + impl
- `SessionManager` — holds DBK alongside VEK; opens DB on unlock, closes on lock
- `DatabaseModule` Hilt DI — provides Provider, Repository
- SQLCipher PRAGMAs: `cipher_memory_security=ON`, `secure_delete=ON`
- `fallbackToDestructiveMigration()` for MVP
- `SupportFactory.setClearPassphrase(true)` — zero DBK after use
- WAL/SHM cleanup on close via `SecureWipe.wipeFile()`

### Out of Scope
- `AlarmEntity` / plain Room DB (separate change)
- Vault UI (browse, upload, view files)
- BLAKE2b integrity checksum
- SQLCipher 4.6.x upgrade
- Panic wipe (references DB but covered separately)

## Capabilities

### New Capabilities
- `vault-database`: Encrypted Room database for vault file metadata. Covers entity, DAO, provider, repository, Hilt module, SQLCipher config, and DBK lifecycle.

### Modified Capabilities
None — no spec-level behavior change. `auth-pin` already returns DBK; this change consumes it.

## Approach

Approach 1 from exploration: `VaultDatabaseProvider` singleton + SQLCipher `SupportFactory`. Provider exposes `open(byte[] dbk)`, `close()`, `getDatabase()`. SessionManager calls open on unlock, close on lock. DAO consumers inject Provider and handle null. DB filename disguised as `timely_cache.db`.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `data/db/VaultDatabase.kt` | New | Room DB + SupportFactory |
| `data/db/entity/VaultFileEntity.kt` | New | Room entity per plam.md §10 |
| `data/db/dao/VaultFileDao.kt` | New | CRUD + queries |
| `data/db/VaultDatabaseProvider.kt` | New | Singleton lifecycle holder |
| `data/repository/VaultRepositoryImpl.kt` | New | Repository impl |
| `domain/repository/VaultRepository.kt` | New | Repository interface |
| `di/DatabaseModule.kt` | New | Hilt module |
| `core/session/SessionManager.kt` | Modified | Add DBK field + lifecycle calls |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| SQLCipher key mismatch | Low | Same DBK ByteArray passed directly |
| Thread safety on Provider mutable state | Med | Synchronized access on open/close/getDatabase |
| Room migration with encrypted DB | Med | fallbackToDestructiveMigration for MVP |
| WAL/SHM artifacts after close | Low | SecureWipe.wipeFile() on .db-wal and .db-shm |

## Rollback Plan

Remove `DatabaseModule` Hilt binding. Revert `SessionManager` changes. Delete `data/db/` package and `VaultRepository`.

## Dependencies

- `room-runtime 2.6.1`, `room-ktx 2.6.1`, `room-compiler` — already in `libs.versions.toml`
- `net.zetetic:android-database-sqlcipher:4.5.4` — already in `libs.versions.toml`
- Auth layer already delivers DBK via `UnlockResult.Success`

## Success Criteria

- [ ] DB opens with correct DBK, closes cleanly, re-opens in same session
- [ ] VaultFileEntity insert/query/delete round-trip works with encrypted DB
- [ ] DAO queries (category, folder, date order) return correct results
- [ ] WAL/SHM files cleaned on close (verified by file existence check)
- [ ] Provider returns null after close — consumers handle gracefully
