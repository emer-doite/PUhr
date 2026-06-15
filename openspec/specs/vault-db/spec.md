# Vault Database Specification

## Purpose

Encrypted SQLCipher-backed Room database for vault file metadata. Entity, DAO, provider, repository, Hilt wiring, DBK lifecycle, and secure cleanup.

## Requirements

### Requirement: VaultDatabase with SQLCipher (REQ-VAULTDB-01)

The system MUST provide a Room @Database class with SQLCipher via `net.zetetic:android-database-sqlcipher` `SupportFactory(byte[])`. The database file MUST be named `timely_cache.db`. `PRAGMA cipher_memory_security=ON` and `PRAGMA secure_delete=ON` MUST be applied via Room callback. `fallbackToDestructiveMigration()` MUST be used for MVP.

#### Scenario: Opens with correct DBK
- GIVEN a valid 32-byte DBK
- WHEN VaultDatabaseProvider.open(dbk) is called
- THEN the database is created and accessible

#### Scenario: Wrong key fails
- GIVEN an invalid DBK
- WHEN VaultDatabaseProvider.open(wrongDbk) is called
- THEN an error is thrown and the database is not accessible

#### Scenario: Destructive migration on schema change
- GIVEN an existing database with an old schema
- WHEN a newer schema version is provided
- THEN the database is destructively recreated

### Requirement: VaultFileEntity (REQ-VAULTDB-02)

The Room entity MUST include: `id` (Long, auto-generate), `fileName` (String), `mimeType` (String), `size` (Long), `encryptedBlob` (ByteArray), `createdAt` (Long, epoch ms), `modifiedAt` (Long, epoch ms), `isFavorite` (Boolean, default false), `tags` (List<String> with TypeConverter).

#### Scenario: Insert auto-generates id
- GIVEN a VaultFileEntity without id
- WHEN inserted
- THEN the returned id is non-null and matches the stored entity

#### Scenario: Tags round-trip
- GIVEN a VaultFileEntity with ["work", "important"]
- WHEN inserted and retrieved
- THEN tags match the original list

### Requirement: VaultFileDao (REQ-VAULTDB-03)

The DAO MUST provide: `insert()` returning Long, `getAll()` as `Flow<List<VaultFileEntity>>` ordered by createdAt DESC, `getById(id)`, `delete(entity)`, `search(query)` by fileName LIKE, `updateFavorite(id, Boolean)`, `updateTags(id, List<String>)`. All mutation methods MUST be suspend functions.

#### Scenario: Write-then-read
- GIVEN an inserted entity
- WHEN getAll is collected
- THEN the entity appears in the emitted list

#### Scenario: Search by partial filename
- GIVEN entities "photo.jpg" and "note.txt"
- WHEN search("photo") is called
- THEN only "photo.jpg" matches

#### Scenario: Toggle favorite
- GIVEN an entity with isFavorite=false
- WHEN updateFavorite(id, true) is called
- THEN getById returns isFavorite=true

### Requirement: VaultDatabaseProvider (REQ-VAULTDB-04)

A @Singleton provider with `synchronized open(dbk)`, `close()`, `getDatabase(): VaultDatabase?`. Returns null after close. `SupportFactory.setClearPassphrase(true)` zeroes the DBK. After close(), `.db-wal` and `.db-shm` MUST be wiped via `SecureWipe.wipeFile()`.

#### Scenario: Open-close cycle
- GIVEN a closed provider
- WHEN open(dbk) is called
- THEN getDatabase() returns non-null
- AND after close(), getDatabase() returns null

#### Scenario: WAL/SHM cleanup
- GIVEN an open database with WAL files
- WHEN close() is called
- THEN .db-wal and .db-shm are wiped and no longer exist

#### Scenario: Double open is idempotent
- GIVEN an already open database
- WHEN open(dbk) is called again
- THEN no error is thrown; the existing instance is returned

### Requirement: VaultRepository (REQ-VAULTDB-05)

`VaultRepository` interface + `VaultRepositoryImpl` wrapping the DAO. All methods return empty-safe defaults when the database is closed rather than throwing.

#### Scenario: Delegates to DAO
- GIVEN VaultRepositoryImpl with an open provider
- WHEN getAll() is collected
- THEN the DAO's getAll Flow is consumed

#### Scenario: Closed provider returns empty
- GIVEN a closed provider
- WHEN getAll() is collected
- THEN an empty list is emitted

### Requirement: SessionManager DBK Integration (REQ-VAULTDB-06)

SessionManager MUST hold DBK alongside VEK. On unlock, calls `VaultDatabaseProvider.open(dbk)`. On lock, calls `close()`. DBK MUST never be persisted; zeroed via `SupportFactory.setClearPassphrase(true)`.

#### Scenario: Unlock opens DB
- GIVEN a locked session
- WHEN unlock produces a valid DBK
- THEN VaultDatabaseProvider.open(dbk) is called
- AND the database is accessible

#### Scenario: Lock closes DB
- GIVEN an open vault database
- WHEN session is locked
- THEN VaultDatabaseProvider.close() is called
- AND getDatabase() returns null

### Requirement: Hilt DatabaseModule (REQ-VAULTDB-07)

A Hilt @Module installed in `SingletonComponent` MUST @Provides `VaultDatabaseProvider` as singleton and bind `VaultRepository` to `VaultRepositoryImpl`.

#### Scenario: Compile resolves all bindings
- GIVEN DatabaseModule installed
- WHEN assembleDebug compiles
- THEN all vault-db DI bindings resolve without errors
