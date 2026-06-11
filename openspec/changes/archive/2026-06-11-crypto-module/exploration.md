## Exploration: crypto-module

### Current State

The PUhr project has an Android scaffold in place with:
- Hilt DI + Compose + Material 3 (dark theme only)
- Clock face implementation (`PUhr.clock.*`) — analog clock + digital readout
- Application class (`PUhr.PhantomVaultApp`) with `@HiltAndroidApp`
- `MainActivity` with `@AndroidEntryPoint`, edge-to-edge, custom color palette
- Package root: `PUhr` (not `com.phantomvault`)
- Tests in `src/test/java/PUhr/` using JUnit 4 (no JUnit 5, no MockK yet)
- No `di/`, `core/`, `data/`, or `domain/` packages exist yet

### Affected Areas

- `PUhr.core.crypto.AesGcmCipher` — NEW: AES-256-GCM encrypt/decrypt
- `PUhr.core.crypto.ArgonKeyDeriver` — NEW: Argon2id key derivation
- `PUhr.core.crypto.KeystoreManager` — NEW: Android Keystore key wrap/unwrap
- `PUhr.core.crypto.SecureWipe` — NEW: 3-pass file overwrite before delete
- `PUhr.core.crypto.SaltGenerator` — NEW: SecureRandom salt factory
- `PUhr.di.CryptoModule` — NEW: Hilt module binding crypto deps
- `app/build.gradle.kts` — may need test deps (MockK, JUnit5) added
- `gradle/libs.versions.toml` — argon2kt=1.6.0 already present; sqlcipher=4.5.4 present

### Approaches

1. **Package structure: `PUhr.core.crypto` vs flat `PUhr.crypto`**
   - **Flat** (like existing `PUhr.clock`): simpler, consistent with current code
   - **`PUhr.core.crypto`**: matches blueprint §4 structure, signals foundational layer, separates from presentation
   - Effort: Low. **Recommend `PUhr.core.crypto`** — Clean Architecture needs the `core` boundary

2. **AesGcmCipher: concrete class vs interface + impl**
   - **Concrete only**: `class AesGcmCipher { fun encrypt(data, key, iv): ByteArray }` — minimal indirection
   - **Interface + impl**: `interface DataEncryptor { ... }` + `class AesGcmCipher : DataEncryptor` — testable via mock
   - **Streaming encryptor**: `suspend fun encrypt(input: InputStream, output: OutputStream)` — good for large files but adds async complexity
   - Effort: Low/Medium. **Recommend concrete class for first slice** — the abstraction boundary lives at `EncryptedFileStore` level, not inside crypto primitives. The primitives are leaf nodes by design.

3. **KeystoreManager: `object` vs `@Singleton class`**
   - **`object`** — as shown in blueprint §9.3 — zero boilerplate, works as Kotlin singleton
   - **`@Singleton class`** — injectable via Hilt, mockable in tests, follows existing Hilt patterns
   - Effort: Low. **Recommend `@Singleton class` with `@Inject constructor()`** — testability matters for a component that wraps Android Keystore. The blueprint code is illustrative, not final.

4. **SecureWipe: file-level vs stream-level vs NIO**
   - **File-level (RandomAccessFile)** — matches blueprint §12.6 — simple, synchronous
   - **NIO (FileChannel + ByteBuffer)** — cleaner API, better error handling, no need for RandomAccessFile
   - **Stream-level** — more general but caller must manage stream lifecycle
   - Effort: Low. **Recommend NIO-based approach using `FileChannel`** — it's the modern Java I/O API, avoids RandomAccessFile's checked exceptions, and supports `force()` for fsync guarantees.

5. **SessionManager: include in this change or defer**
   - **Include** — "crypto module" naturally includes session management? SessionManager depends on crypto types but is a consumer, not a primitive
   - **Defer** — SessionManager belongs in `core/session/` (blueprint §4 structure), not `core/crypto/`. It depends on the crypto module but doesn't define it.
   - Effort: N/A for this decision. **Recommend defer** — SessionManager is a higher-level component that consumes crypto primitives; it's a domain/data layer concern, not a foundation primitive.

6. **Hilt module: `@Provides` vs constructor injection**
   - **Constructor injection**: add `@Inject constructor` to each class and bind in module
   - **`@Provides` in CryptoModule**: manually instantiate and provide each
   - **`@Binds` with interfaces**: requires interface abstraction
   - Effort: Low. **Recommend constructor injection where possible** (KeystoreManager, ArgonKeyDeriver), and `@Provides` for stateless utilities (AesGcmCipher, SaltGenerator). This keeps the module thin.

7. **Test strategy: JVM vs instrumented vs hybrid**
   - **JVM-only**: Simple but Argon2kt JNI won't work on desktop JVM (no native .so)
   - **Hybrid**: AesGcmCipher (JVM), SaltGenerator (JVM), SecureWipe (JVM) — ArgonKeyDeriver (instrumented), KeystoreManager (instrumented)
   - **All instrumented**: Consistent but slow — unnecessary for pure computation
   - Effort: Medium. **Recommend hybrid** — JVM unit tests for pure computation (AesGcmCipher, SaltGenerator, SecureWipe), instrumented for platform-dependent (KeystoreManager, ArgonKeyDeriver). Current test framework is JUnit 4; no changes needed for JVM tests.

### Recommendation

**Package**: `PUhr.core.crypto` — follows blueprint §4 structure, separates foundation from presentation.

**Crypto primitives as concrete classes** (no interface layer) — the abstraction boundary is at `EncryptedFileStore`/`UseCase` level. The primitives are leaf nodes.

**`KeystoreManager` as `@Singleton class`** — testability via Hilt mock injection outweighs `object` simplicity.

**`SecureWipe` as a top-level utility with NIO** — `object SecureWipe { fun wipeFile(file: Path, passes: Int = 3) }` using `FileChannel` for fsync guarantees.

**SessionManager deferred** — it's a `core/session/` concern, not `core/crypto/`.

**Hybrid testing** — JVM for computation, instrumented for platform-dependent.

### Risks

- **Argon2kt JNI won't load on desktop JVM** → ArgonKeyDeriver tests MUST be instrumented. If instrumented tests aren't feasible in current CI setup, push Argon2kt tests to a separate verify step.
- **`setUnlockedDeviceRequired(true)` on API < 30** may cause issues — needs runtime SDK check (fallback for API 28-29)
- **No MockK in current dependencies** → unit tests for crypto primitives are integration-style (real encrypt/decrypt) rather than mock-based, which is actually preferred for crypto
- **IV uniqueness** — GCM fails catastrophically if IV+key pair is reused. The caller (`EncryptedFileStore`) must ensure unique IV per file. AesGcmCipher should document this contract.
- **argon2kt version mismatch** — blueprint says 2.0.0, actual `libs.versions.toml` says 1.6.0. The API may differ. Verify 1.6.0 API before implementation.

### Scope Boundary

**In scope (first slice)**:
- `PUhr.core.crypto.AesGcmCipher` — encrypt/decrypt ByteArray with AES-256-GCM
- `PUhr.core.crypto.ArgonKeyDeriver` — derive 256-bit key from PIN + salt via Argon2id
- `PUhr.core.crypto.KeystoreManager` — generate/retrieve/delete Android Keystore wrapping key
- `PUhr.core.crypto.SecureWipe` — NIO-based 3-pass file overwrite
- `PUhr.core.crypto.SaltGenerator` — 128-bit SecureRandom salt
- `PUhr.di.CryptoModule` — Hilt module providing crypto dependencies
- JVM unit tests for AesGcmCipher, SecureWipe, SaltGenerator
- Instrumented tests for KeystoreManager, ArgonKeyDeriver

**Out of scope (next changes)**:
- `core/session/SessionManager` — in-memory session key cache with auto-lock
- `core/session/AutoLockTimer` — timer-based session expiration
- `EncryptedFileStore` — the file-level encryption layer that consumes AesGcmCipher
- `DatabaseModule` — provides SQLCipher with DBK
- Biometric integration — Keystore-backed biometric key release

### Dependency Realities (vs Blueprint)

| Dependency | Blueprint Value | Actual `libs.versions.toml` | Impact |
|---|---|---|---|
| argon2kt | 2.0.0 | 1.6.0 | API surface may differ — `Argon2kt()` constructor vs builder pattern |
| sqlcipher | 4.5.6 | 4.5.4 | Minor; test with actual version |
| JUnit | JUnit 5 | JUnit 4.13.2 | Tests use JUnit 4 — no change needed |

### Ready for Proposal
Yes — the approach is clear, risks are documented, and the scope boundary is well-defined. Proceed to sdd-propose.
