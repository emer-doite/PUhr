## Exploration: Vault Database (vault-db)

### Current State
- **Auth layer complete**: `AuthRepositoryImpl` derives 64B from Argon2id, splits [0..31]=VEK, [32..63]=DBK. `UnlockResult.Success(dbk)` returns DBK to caller. `SessionManager` holds VEK.
- **No DB layer exists**: No `data/db/` package, no entities, no DAOs, no `VaultDatabase.kt`.
- **Dependencies present**: `room-runtime 2.6.1`, `room-ktx 2.6.1`, `room-compiler`, `net.zetetic:android-database-sqlcipher:4.5.4` all in `libs.versions.toml` + `build.gradle.kts`.
- **DBK is orphaned**: Returned from `UnlockResult.Success` but not consumed anywhere.
- **No Hilt DatabaseModule** exists yet.

### Affected Areas
- `app/src/main/java/PUhr/data/db/VaultDatabase.kt` — new: Room + SQLCipher database class
- `app/src/main/java/PUhr/data/db/entity/VaultFileEntity.kt` — new: Room entity per plam.md §10
- `app/src/main/java/PUhr/data/db/dao/VaultFileDao.kt` — new: CRUD + queries
- `app/src/main/java/PUhr/data/db/VaultDatabaseProvider.kt` — new: lifecycle-aware DB holder
- `app/src/main/java/PUhr/di/DatabaseModule.kt` — new: Hilt module for DB provider
- `app/src/main/java/PUhr/core/session/SessionManager.kt` — SHOULD add DBK alongside VEK
- `gradle/libs.versions.toml` — already has deps; MAY need `cipher-migration-test dep`
- `app/src/main/java/PUhr/domain/repository/VaultRepository.kt` — new interface for vault file operations

---

### Approaches

#### 1. VaultDatabaseProvider Singleton + SupportFactory
Room uses `SupportSQLiteOpenHelper.Factory`. SQLCipher's `net.sqlcipher.database.SupportFactory` implements this directly. Create a `VaultDatabaseProvider` (@Singleton) with explicit `open(dbk)` and `close()` methods. On unlock: `Provider.open(dbk)`. On lock: `Provider.close()`. All DAO consumers inject Provider and call `getDatabase()` which returns null when closed — callers must handle null/closed state.

| Pros | Cons | Effort |
|------|------|--------|
| Idiomatic Room + SQLCipher — zero hacks | Callers must handle null database state | Low |
| Clean lifecycle — open/close is explicit | Provider becomes a mutable singleton (less pure DI) | |
| `SupportFactory` is SQLCipher's documented API for Room | Need to synchronize access across threads | |
| Easy to test with in-memory SupportFactory + key | | |
| No changes to existing auth flow — DBK already returned from UnlockResult | | |

#### 2. Hilt @VaultScope with Custom Component + SessionManager DBK
Create a custom `@VaultScope` + Hilt `@VaultComponent` created on unlock and destroyed on lock. Database, DAOs, and VaultRepository are all scoped to this component. SessionManager extended to hold both VEK and DBK. On lock: component destroyed, all scoped instances GC'd.

| Pros | Cons | Effort |
|------|------|--------|
| Pure DI — no mutable singletons | Complex Hilt custom component setup | High |
| Database never accessible outside session | Custom components need manual creation (no @HiltViewModel auto-wiring) | |
| No null checks needed | Component destruction must be explicitly wired; easy to miss | |
| DAOs and Repository scoped to session | Overkill for MVP — auth layer doesn't use session scope | |

#### 3. Two-Database Design (Plain Room for Alarms + SQLCipher for Vault)
Same as Approach 1 for vault DB, plus a second unencrypted Room database (`ClockDatabase`) for `AlarmEntity`. Vault DB only contains `VaultFileEntity`. Clock DB always accessible (no auth needed) — alarms must show on clock screen without unlocking.

| Pros | Cons | Effort |
|------|------|--------|
| Alarms work without vault unlock | Two Hilt database providers needed | Medium |
| Clean separation (disguise vs vault) | More complex DI wiring | |
| Follows disguise principle: clock data is innocent | AlarmEntity in plam.md §10 sits under VaultDatabase — spec vs practicality conflict | |
| Vault DB can be absent when vault locked | | |

---

### Recommendation
**Approach 1 (VaultDatabaseProvider Singleton + SupportFactory)** for the first slice.

**Rationale:**
- DBK already flows through `UnlockResult.Success` — just needs to be passed to a provider
- `SupportFactory(dbKey)` is a one-liner — Room handles everything else
- Avoids Hilt custom component complexity (Approach 2) not yet justified
- AlarmEntity question (Approach 3) is separate — vault-db focuses on vault entities first

### Key Decisions
1. **DBK lifecycle**: Stored in `SessionManager` alongside VEK. Opens DB on unlock, closes on lock. Survives config changes within session.
2. **AlarmEntity out of scope**: Alarm data belongs in a separate plain Room DB (separate change). Vault-db only covers vault entities.
3. **Database filename**: Disguised name like `timely_cache.db` — never `vault.db`.
4. **Migration**: `fallbackToDestructiveMigration()` for MVP development. Proper `Migration` objects before production release.
5. **SQLCipher defaults**: Accept defaults initially. Add `PRAGMA cipher_memory_security = ON` and `PRAGMA secure_delete = ON` via Room callback.

### Anti-Forensics Requirements
- **WAL/SHM cleanup**: After `db.close()`, delete WAL/SHM files with `SecureWipe.wipeFile()`
- **Secure delete**: `PRAGMA secure_delete = ON` — freed pages zeroed
- **Memory**: `SupportFactory.setClearPassphrase(true)` clears DBK ByteArray after use
- **Panic wipe**: `SecureWipe.wipeFile(dbPath)` before `deleteDatabase()` — already in plam.md `PanicWipeUseCase`
- **`.nomedia`**: Already in place for vault directory
- **Backup exclusion**: Already in manifest

### Open Questions
1. **AlarmEntity home**: Separate plain Room DB (`ClockDatabase`) or accept encrypted-only? Disguise principle says alarms must work without auth.
2. **DBK in SessionManager**: Hold DBK directly (more key material in memory) or re-derive on config change? Recommended: hold it.
3. **Migration testability**: Room migration tests need a keyed SQLCipher DB. Does `SupportFactory` support `:memory:` databases for test fixtures? Needs verification.
4. **SQLCipher 4.5.4 → 4.6.x**: Should we upgrade for security fixes? Requires testing.
5. **SupportFactory passphrase format**: `byte[]` (current) vs `char[]` (SQLCipher recommendation for memory control). Add conversion?
6. **DB integrity**: BLAKE2b checksum on DB open (plam.md §10) — Room callback vs separate check in VaultRepository?
