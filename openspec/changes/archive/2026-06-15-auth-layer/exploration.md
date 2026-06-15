# Exploration: Authentication Layer (auth-layer)

## Current State

### What exists
- **Crypto primitives**: `KeyDeriver` interface + `ArgonKeyDeriver` (Argon2id, 65536KB/3/4, 256-bit output), `SaltGenerator` (128-bit), `KeystoreManager` (AES-256-GCM wrapping key in Android Keystore), `AesGcmCipher` (AES-256-GCM `"PVEN"` header format), `SecureWipe` (3-pass NIO wipe)
- **Session management**: `SessionManager` (in-memory VEK, `ByteArray` zero-fill on close), `AutoLockTimer` (coroutine-based, configurable timeout)
- **Clock disguise**: `ClockScreen`, `AnalogClockFace`, `ClockViewModel` (sealed `ClockMode`)
- **DI**: `CryptoModule` binds `KeyDeriver → ArgonKeyDeriver`, `@Singleton` pattern for crypto classes

### What needs to be built for auth-layer
- **Secret gesture detector**: Bridge from clock to auth — compound gesture (triple-tap center → swipe left → long-press 6 o'clock) on clock screen
- **Auth UI**: `AuthScreen` (PIN entry), `PinPad` composable (custom numeric pad), `BiometricPromptLauncher`, `AuthViewModel`
- **Auth domain**: `AuthRepository` (salt + verification hash + duress PIN storage), `UnlockVaultUseCase`, `ValidateDuressUseCase`, `ChangePinUseCase`
- **Navigation**: `NavGraph` with `ClockScreen` as start, hidden `VaultEntryScreen` → `AuthScreen` → `VaultHomeScreen`
- **Key splitting**: MasterKey (256-bit from Argon2id) → VEK (file encryption) + DBK (SQLCipher); 256 bits isn't enough for both — need 512-bit output or HKDF expansion

### Key gap: no split-key strategy
Current `ArgonKeyDeriver` produces 32 bytes (256 bits). Blueprint needs 64 bytes (32 VEK + 32 DBK). Must either increase `hashLengthInBytes` to 64 or add HKDF-based key expansion.

### Key gap: no verification mechanism
PIN is verified by comparing against a hash of the derived key, but no storage layer exists for salt + verification hash + throttle metadata.

---

## Affected Areas

- `app/src/main/java/PUhr/core/crypto/ArgonKeyDeriver.kt` — increase output to 64 bytes OR add HKDF expander
- `PUhr/domain/usecase/auth/UnlockVaultUseCase.kt` — new file; orchestrate PIN → derive → verify → session
- `PUhr/domain/usecase/auth/ChangePinUseCase.kt` — new file; re-derive, re-verify, re-store
- `PUhr/domain/usecase/auth/ValidateDuressUseCase.kt` — new file; duress PIN detection
- `PUhr/domain/repository/AuthRepository.kt` — new interface for auth metadata persistence
- `PUhr/data/repository/AuthRepositoryImpl.kt` — new impl; EncryptedSharedPreferences or DataStore
- `PUhr/presentation/auth/AuthScreen.kt` — new file; PIN entry screen
- `PUhr/presentation/auth/AuthViewModel.kt` — new file; auth state machine
- `PUhr/presentation/auth/components/PinPad.kt` — new file; custom numeric pad
- `PUhr/presentation/auth/components/BiometricPromptLauncher.kt` — new file; biometric integration
- `PUhr/clock/gesture/SecretGestureDetector.kt` — new file; compound gesture trigger
- `PUhr/clock/ClockScreen.kt` — add gesture detector + `onTriggerDetected` callback
- `PUhr/MainActivity.kt` — add NavHost navigation
- `PUhr/di/RepositoryModule.kt` — new Hilt module for auth bindings

---

## Approaches

### Approach 1: EncryptedSharedPreferences for metadata, 64-byte Argon2id output

Store salt, verification hash, duress PIN hash, and throttle state in EncryptedSharedPreferences. Increase Argon2id output to 64 bytes for direct VEK+DBK split.

| Pros | Cons | Effort |
|------|------|--------|
| No extra DB dependency | EncryptedSharedPreferences uses AndroidKeyStore backing, works API 23+ | Medium |
| Simple key-value API | Throttle counters need atomic updates | |
| Survives app data clear (intentional) | 64-byte Argon2id derivation costs 2× memory (still acceptable at 64MB) | |
| Invisible to casual inspection | | |

### Approach 2: HKDF expansion from 256-bit Argon2id output

Keep Argon2id at 32 bytes. Add `HkdfExpander` utility using `javax.crypto.Mac` with HmacSHA256 to derive VEK and DBK via `HKDF-Expand(masterKey, "vek")` and `HKDF-Expand(masterKey, "dbk")`.

| Pros | Cons | Effort |
|------|------|--------|
| No change to existing Argon2id params | New crypto primitive to implement/test | Medium |
| Follows NIST SP 800-56C recommendation | Extra verification step needed beyond KDF | |
| Clean domain separation per sub-key | Must ensure constant-time comparison for verification | |
| HKDF-Expand is standard, auditable | | |

### Approach 3: Keystore-wrapped master key with 256-bit Argon2id + separate DBK storage

Reuse existing KeystoreManager. Derive 32 bytes from Argon2id → "auth key". Wrap auth key in Keystore. Generate random VEK (32 bytes) and DBK (32 bytes), encrypt both with auth key via AesGcmCipher, store alongside salt. On return: derive auth key from PIN, decrypt stored VEK+DBK, verify integrity via GCM auth tag.

| Pros | Cons | Effort |
|------|------|--------|
| Full use of existing primitives | More moving parts — encrypt/decrypt VEK+DBK at rest | High |
| 256-bit Argon2id keeps compute cost lower | Harder to reason about verification flow | |
| GCM auth tag acts as implicit PIN verification (decrypt fails on wrong PIN) | Biometric integration adds another wrapping layer | |
| No hash verification needed — GCM auth tag serves as implicit PIN check | | |

### Approach 4: Filesystem-based JSON for metadata (not recommended)

Store salt, verification hash, throttle state as plain JSON in `context.filesDir`. No encryption at rest.

| Pros | Cons | Effort |
|------|------|--------|
| Simplest I/O | Plaintext on disk — defeats the purpose | Low |
| Easy to inspect/debug | Any app with file access (rooted device) reads auth metadata | |
| | Violates zero-knowledge principle | |

---

## Recommendation

**Approach 1** (EncryptedSharedPreferences + 64-byte Argon2id) for the first slice.

Rationale:
- Direct split avoids HKDF complexity (Approach 2) in the first version
- EncryptedSharedPreferences has minimal surface area — it's just key-value with AES-256 at rest, Keystore-bound
- 64MB Argon2id memory cost is acceptable; derivation time is the same regardless of output length
- Throttle counters are simple CRUD operations
- Avoids chicken-and-egg problem of needing DBK to open Room before getting auth metadata (Approach 3)

The 64-byte derivation is safe — Argon2id uses BLAKE2b internally in counter mode for arbitrary-length output, producing independent pseudo-random blocks.

### PIN Verification

```
hash(derive(pin, salt)[0..63]) → storedVerificationHash
```

Compare with constant-time `MessageDigest.isEqual()` on return. No timing side-channel.

### First-time setup flow

```
generate salt → derive(pin, salt) → split [0..31]=VEK, [32..63]=DBK 
→ SHA-256(VEK + DBK) = verificationHash 
→ store(salt, verificationHash) in EncryptedSharedPreferences 
→ openSession(VEK)
```

### Returning user flow

```
read salt from prefs → derive(pin, salt) → split VEK+DBK 
→ SHA-256(VEK + DBK) == stored verificationHash? 
→ YES: openSession(VEK), return DBK 
→ NO: increment failCount, return null
```

---

## Risks

- **Min SDK 28**: `EncryptedSharedPreferences` available via AndroidX Security (API 23+). Must add `androidx.security:security-crypto` dependency. Compatible.
- **Argon2id on older devices**: Argon2id with 64MB memory + 3 iterations may take 2–5 seconds on low-end devices (API 28/29). Acceptable per blueprint's Appendix A (>300ms is acceptable). Consider lower memory variant for API <30 in a future optimization.
- **GCM nonce reuse**: Not applicable here — we're using Argon2id, not directly encrypting with user-controlled input.
- **PinPad security**: Custom composable must not log key events, must not expose digit count as empty slots. Blueprint says "dots grow as user types."
- **Throttle state persistence**: If EncryptedSharedPreferences file is deleted, throttle resets. Acceptable risk — user loses their vault anyway if prefs are tampered with (salt gone = cannot derive key).

---

## Scope Boundary (First Slice)

### In scope (auth-layer change)
1. AuthRepository (interface + EncryptedSharedPreferences impl) — salt, verification hash, fail count, duress PIN hash
2. UnlockVaultUseCase — PIN → derive → verify → split → open session
3. ChangePinUseCase — re-derive, re-verify, update stored hash
4. AuthViewModel — sealed UiState (PinEntry, Authenticating, Verified, Error, FirstTimeSetup)
5. AuthScreen — full Compose screen with PinPad
6. PinPad — custom composable, digits 0–9, backspace, no system keyboard
7. Increase ArgonKeyDeriver output to 64 bytes
8. Verification via SHA-256 of derived material, compared with `MessageDigest.isEqual()`
9. Throttling — 5-fail cooldown (30s double), 15-fail optional wipe trigger (wired but wipe action out of scope)
10. Hilt bindings for AuthRepository → AuthRepositoryImpl, UnlockVaultUseCase, etc.

### Out of scope (separate changes)
- **Biometric integration** (auth-biometric or Phase 2) — needs KeystoreKeygenParameterSpec with `setUserAuthenticationRequired(true)`, BiometricPrompt, CryptoObject
- **Duress PIN separate UI** (auth-duress or Phase 2) — needs DuressScreen, decoy file system
- **SecretGestureDetector** (hidden-entry — separate change) — gesture composable, timer reset logic
- **Navigation graph** (navigation-change) — NavHost, NavGraph, route constants
- **Vault database creation** (vault-db) — Room + SQLCipher, VaultDatabase, DAOs
- **Auto-lock wiring** — connecting AutoLockTimer to SessionManager close (belongs in session-integration)
- **Panic wipe** — PanicWipeUseCase, SecureWipe integration

---

## Next Step

**sdd-propose** — Formalize the scope, approach, and rollback plan for the auth-layer change using the recommended Approach 1. The proposal should define:
- Exact EncryptedSharedPreferences schema (keys for salt, verificationHash, failCount, duressPinHash)
- Argon2id parameter confirmation (65536KB/3/4, 64-byte output)
- PinPad API surface (digits, backspace, submit, shake animation)
- AuthViewModel state machine transitions
- Throttle timing formula
- Rollback for initial setup: delete prefs file = reset
