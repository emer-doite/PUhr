# Tasks: Crypto Module — Core Cryptographic Primitives

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~595 (260 impl + 330 tests + 5 build) |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1: Foundation + Session → PR 2: Core crypto + Hilt → PR 3: tests |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending — ask user |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Base | Notes |
|------|------|-----------|------|-------|
| 1 | KeyDeriver, SecureWipe, SaltGenerator, SessionManager, AutoLockTimer + JVM tests + CryptoModule | PR 1 | main | ~195 lines, standalone delivery |
| 2 | AesGcmCipher, ArgonKeyDeriver, KeystoreManager + instrumented tests | PR 2 | main | ~210 lines, no cross-deps on unit 1 |
| 3 | All instrumented tests + build.gradle.kts final verification | PR 3 | main | ~190 lines |

## Phase 1: Foundation

- [x] 1.1 Create `PUhr/core/crypto/KeyDeriver.kt` — interface with `derive(pin: String, salt: ByteArray): ByteArray`
- [x] 1.2 Create `PUhr/core/crypto/SaltGenerator.kt` — `@Singleton class` wrapping `SecureRandom` for 16-byte salt (deviation from design: class instead of object for Hilt injection)
- [x] 1.3 Create `PUhr/core/crypto/SecureWipe.kt` — `object` with NIO FileChannel 3-pass overwrite + `force()` + delete (deviation from design: suspend function with Dispatchers.IO)

## Phase 2: Core Implementation

- [x] 2.1 Create `PUhr/core/crypto/AesGcmCipher.kt` — class with `encrypt()`/`decrypt()`, IV prepended to ciphertext, EncryptedData model with "PVEN" header
- [x] 2.2 Create `PUhr/core/crypto/ArgonKeyDeriver.kt` — implements `KeyDeriver` via argon2kt 1.6.0 (Argon2id, m=65536, t=3, p=4)
- [x] 2.3 Create `PUhr/core/crypto/KeystoreManager.kt` — `@Singleton` with `generateWrappingKey()`, `wrapKey()`, `unwrapKey()`, `deleteKey()`, `hasKey()`, API 30+ guard
- [x] 2.4 Create `PUhr/core/session/SessionManager.kt` — in-memory VEK holder, zero-fill `closeSession()`
- [x] 2.5 Create `PUhr/core/session/AutoLockTimer.kt` — coroutine delay (30s–5min) → `closeSession()`, cancellable

## Phase 3: Integration / Wiring

- [x] 3.1 Create `PUhr/di/CryptoModule.kt` — Hilt `@Module` with `@Binds` for `KeyDeriver` interface, `@Singleton` scoping
- [x] 3.2 Modify `app/build.gradle.kts` — add `androidTestImplementation` deps for instrumented tests

## Phase 4: Testing

- [x] 4.1 Create `PUhr/core/crypto/FakeKeyDeriver.kt` (SHA-256) + `KeyDeriverContractTest.kt` covering REQ-CRYPTO-02
- [x] 4.2 Write `AesGcmCipherTest.kt` — round-trip (REQ-CRYPTO-01 S1) + tampered ciphertext → AEADBadTagException (S2)
- [x] 4.3 Write `SaltGeneratorTest.kt` — 16-byte output (REQ-CRYPTO-05 S1) + successive salts diverge (S2)
- [x] 4.4 Write `SecureWipeTest.kt` — wipe+delete (REQ-CRYPTO-04 S1) + non-existent path no-op (S2)
- [x] 4.5 Write `SessionManagerTest.kt` — open/retrieve (REQ-CRYPTO-06 S1) + close zero-fill (S2) + safe double-close (S3)
- [x] 4.6 Write `AutoLockTimerTest.kt` — timer fires locks (REQ-CRYPTO-07 S1) + manual close cancels (S2)
- [x] 4.7 Write `ArgonKeyDeriverInstrumentedTest.kt` — same PIN+salt (REQ-CRYPTO-02 S1) + different salts (S2)
- [x] 4.8 Write `KeystoreManagerInstrumentedTest.kt` — key generation (REQ-CRYPTO-03 S1) + API 28-29 fallback (S2) + cipher retrieval (S3)
