# Design: Crypto Module

## Technical Approach

Package `PUhr.core.crypto` with concrete leaf-node primitives behind a `KeyDeriver` interface (for Argon2kt JNI isolation). `KeystoreManager` as `@Singleton class` via Hilt for testability. `SecureWipe` and `SaltGenerator` as stateless utilities provided via `@Provides`. Hybrid testing: JVM unit tests for pure computation, instrumented tests for platform-dependent.

## Architecture Decisions

### Decision: KeyDeriver interface for Argon2 JNI isolation

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Concrete Argon2kt class | JNI fails on desktop JVM → no JVM tests | ❌ |
| `KeyDeriver` interface + `ArgonKeyDeriver` + `FakeKeyDeriver` | Extra file, but unlocks JVM tests for consumers | ✅ |
| **Rationale**: Argon2kt uses JNI (`System.loadLibrary`). On desktop JVM the native `.so` won't load. Interface lets consumers (like `SessionManager`) be tested on JVM with `FakeKeyDeriver` (SHA-256). Real Argon2id tests live in `androidTest`. |

### Decision: KeystoreManager API 30+ fallback

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Always call `setUnlockedDeviceRequired(true)` | Crashes on API 28-29 (`NoSuchMethodError`) | ❌ |
| Skip conditionally via `Build.VERSION.SDK_INT >= 30` | Key is still hardware-backed on API 28-29 via StrongBox/TEE | ✅ |
| **Rationale**: `setUnlockedDeviceRequired` was added in API 30 (Android 11). minSdk=28 means we must guard it. On 28-29 the key is still generated with `isStrongBoxBacked=true` via TEE. |

### Decision: Concrete primitives, interface only at KeyDeriver

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Interface for every crypto class | Premature abstraction; 5 interfaces + 5 impls for 15-line classes | ❌ |
| Concrete classes, one `KeyDeriver` interface | Abstraction only where it solves a real problem (JNI isolation) | ✅ |
| **Rationale**: Abstractions at leaf nodes add cost without benefit. The boundary is at `EncryptedFileStore`/`UseCase` level. Only `KeyDeriver` needs an interface because Argon2kt can't run on JVM. |

### Decision: SessionManager + AutoLockTimer included in this change

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Defer to separate change | Spec (REQ-CRYPTO-06, REQ-CRYPTO-07) requires them; SessionManager is a thin wrapper over a ByteArray | ❌ |
| Include now | 2 extra files, each ~30 lines; unlocks end-to-end session flow for integration tests | ✅ |
| **Rationale**: Spec and user file changes both include them. SessionManager is a session-key holder with zero-fill on close — minimal complexity, high value. |

### Decision: argon2kt 1.6.0 API confirmed

Verified via `javap` on gradle-cached API jar. `Argon2Kt()` constructor (zero-arg), `hash(mode: Argon2Mode, password: ByteArray, salt: ByteArray, mCost: Int, tCost: Int, parallelism: Int, hashLength: Int, version: Argon2Version)` returns `Argon2KtResult` with `rawHashAsByteArray(): ByteArray`. Default mCost=65536, tCost=3, parallelism=4, hashLength=32, version=V13 — matches spec defaults exactly.

## Data Flow

```
PIN + Salt ──→ ArgonKeyDerider.derive() ──→ 256-bit VEK
                                                  │
                                                  ▼
                                          SessionManager.openSession(VEK)
                                                  │
                         ┌────────────────────────┤
                         │                        │
                         ▼                        ▼
                  AesGcmCipher.encrypt()   AutoLockTimer (countdown)
                  AesGcmCipher.decrypt()   ──→ closeSession() on timeout
                         │
                         ▼
                KeystoreManager.wrapKey(VEK)
                KeystoreManager.unwrapKey() ──→ recover VEK
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `app/src/main/java/PUhr/core/crypto/AesGcmCipher.kt` | Create | AES-256-GCM encrypt/decrypt, concrete class |
| `app/src/main/java/PUhr/core/crypto/ArgonKeyDeriver.kt` | Create | Argon2id via argon2kt 1.6.0, implements `KeyDeriver` |
| `app/src/main/java/PUhr/core/crypto/KeyDeriver.kt` | Create | Interface: `fun derive(pin: String, salt: ByteArray): ByteArray` |
| `app/src/main/java/PUhr/core/crypto/KeystoreManager.kt` | Create | `@Singleton` Android Keystore wrap/unwrap, API 30+ guard |
| `app/src/main/java/PUhr/core/crypto/SecureWipe.kt` | Create | `object` — NIO FileChannel 3-pass overwrite + force() |
| `app/src/main/java/PUhr/core/crypto/SaltGenerator.kt` | Create | `object` — SecureRandom 16-byte salt |
| `app/src/main/java/PUhr/core/session/SessionManager.kt` | Create | In-memory VEK holder, zero-fill on close |
| `app/src/main/java/PUhr/core/session/AutoLockTimer.kt` | Create | Coroutine-based configurable timeout → closeSession |
| `app/src/main/java/PUhr/di/CryptoModule.kt` | Create | Hilt `@Module` binding all crypto deps |
| `app/build.gradle.kts` | Modify | Add `androidTestImplementation` deps if needed |

## Interfaces / Contracts

```kotlin
// PUhr/core/crypto/KeyDeriver.kt
interface KeyDeriver {
    fun derive(pin: String, salt: ByteArray): ByteArray
}

// PUhr/core/crypto/AesGcmCipher.kt
class AesGcmCipher {
    // IV is PREPENDED to ciphertext (12 bytes IV + encrypted data + 16-byte tag)
    fun encrypt(data: ByteArray, key: ByteArray): ByteArray
    fun decrypt(ciphertext: ByteArray, key: ByteArray): ByteArray
}

// PUhr/core/crypto/KeystoreManager.kt
@Singleton
class KeystoreManager @Inject constructor() {
    fun generateWrappingKey(): Boolean        // alias "pv_master_key_wrap"
    fun getCipher(purpose: Int): Cipher       // ENCRYPT_MODE or DECRYPT_MODE
    fun deleteWrappingKey(): Boolean
}
```

## Testing Strategy

| Layer | What | Approach |
|-------|------|----------|
| Unit (JVM) | AesGcmCipher | Real encrypt/decrypt round-trip, tampered ciphertext → `AEADBadTagException` |
| Unit (JVM) | SecureWipe | Temp file 3-pass → bytes differ, file deleted; non-existent path → no-op |
| Unit (JVM) | SaltGenerator | 16-byte output, successive calls diverge |
| Unit (JVM) | SessionManager | Open/get/close sequence; close zero-fills; close with no session → safe |
| Unit (JVM) | AutoLockTimer | `runBlockingTest` with `delay` — timer fires locks, manual close cancels |
| Unit (JVM) | FakeKeyDeriver | Deterministic SHA-256 output for consumer tests |
| Instrumented | ArgonKeyDeriver | Real Argon2id via JNI: same PIN+salt → same key, different salts → different keys |
| Instrumented | KeystoreManager | Generate key in Keystore, retrieve Cipher, delete |
| Compile | CryptoModule Hilt | `./gradlew assembleDebug` — all bindings resolve |

## Open Questions

None. All three mitigations verified (argon2kt 1.6.0 API confirmed via `javap` on cached jar).
