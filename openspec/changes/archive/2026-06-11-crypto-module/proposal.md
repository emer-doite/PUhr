# Proposal: Crypto Module — Core Cryptographic Primitives

## Intent
Implement the cryptographic foundation for the PUhr vault. AES-256-GCM encryption, Argon2id key derivation, Android Keystore key management, and secure file destruction — all data protection depends on this layer.

## Scope

### In Scope
- AesGcmCipher — AES-256-GCM encrypt/decrypt (ByteArray)
- ArgonKeyDeriver — Argon2id PIN→key derivation
- KeystoreManager — Android Keystore wrap/unwrap
- SecureWipe — NIO 3-pass file overwrite
- SaltGenerator — SecureRandom 128-bit salt
- CryptoModule — Hilt DI module binding all crypto deps
- JVM tests: AesGcmCipher, SecureWipe, SaltGenerator
- Instrumented tests: KeystoreManager, ArgonKeyDeriver

### Out of Scope
- SessionManager — deferred to core/session/ change
- AutoLockTimer — deferred with SessionManager
- EncryptedFileStore — higher-level file encryption consumer
- DatabaseModule — SQLCipher provider (separate change)
- Biometric key release — later Keystore integration

## Capabilities

### New Capabilities
- `crypto-module`: Core cryptographic primitives (AES-256-GCM, Argon2id, Android Keystore, secure wipe, salt generation)

### Modified Capabilities
None.

## Approach
Package `PUhr.core.crypto` per blueprint §4. Concrete classes as leaf nodes (abstraction boundary at EncryptedFileStore level). KeystoreManager as `@Singleton class` for Hilt testability. SecureWipe as top-level `object` with NIO FileChannel + `force()` fsync. Hybrid testing: JVM for computation (AesGcmCipher, SecureWipe, SaltGenerator), instrumented for platform-dependent (KeystoreManager, ArgonKeyDeriver). DI via constructor injection, `@Provides` for stateless utilities.

## Affected Areas
| Area | Impact | Description |
|------|--------|-------------|
| PUhr.core.crypto/ | New | 5 crypto primitive files |
| PUhr.di.CryptoModule | New | Hilt module binding crypto deps |
| app/build.gradle.kts | Modified | Add test deps if needed |

## Risks
| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Argon2kt JNI fails on desktop JVM | High | KeyDeriver interface + instrumented-only Argon2 tests |
| setUnlockedDeviceRequired crash on API<30 | Med | Runtime SDK check, skip on 28-29 |
| argon2kt 1.6.0 API differs from 2.0.0 | Low | Verify constructor API at design time |
| IV+key reuse breaks GCM | Low | Document contract: caller MUST provide unique IV |
| No MockK in existing test deps | Low | Integration-style tests preferred for crypto |

## Rollback Plan
1. Delete `PUhr/core/crypto/` and `PUhr/di/CryptoModule.kt`
2. Revert `app/build.gradle.kts` test deps if added
3. Run `./gradlew assembleDebug` to verify clean build

## Dependencies
- argon2kt 1.6.0 (already in `libs.versions.toml`)
- Android Keystore (platform API)
- javax.crypto (JDK standard)

## Success Criteria
- [ ] AesGcmCipher: encrypt-decrypt round-trip succeeds
- [ ] AesGcmCipher: tampered ciphertext throws AEADBadTagException
- [ ] ArgonKeyDeriver: same PIN + salt → identical key
- [ ] ArgonKeyDeriver: different salts → different keys
- [ ] SecureWipe: file bytes randomized after wipe
- [ ] `./gradlew assembleDebug` passes; Hilt compiles all bindings
