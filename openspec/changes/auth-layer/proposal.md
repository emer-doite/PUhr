# Proposal: Auth Layer — PIN Authentication

## Intent

Provide PIN-based vault authentication: derive VEK+DBK from user PIN via Argon2id (512-bit), verify against stored hash, throttle failures progressively, and manage session lifecycle.

## Scope

### In Scope
- AuthRepository (interface + EncryptedSharedPreferences impl) — salt, verification hash, fail count
- ArgonKeyDeriver output: 32 → 64 bytes for direct VEK+DBK split
- UnlockVaultUseCase — PIN → derive → verify → split → open session
- ChangePinUseCase — re-derive, re-verify, update stored hash
- AuthViewModel — sealed UiState (PinEntry, Authenticating, Verified, Error, FirstTimeSetup)
- AuthScreen — Compose PIN screen, dark theme (#0A0A0C), error shake animation
- PinPad — custom numeric composable: 0–9, backspace, submit, no system keyboard, dot-progress (no empty slots)
- PIN verification — SHA-256(derive(pin, salt)) vs stored hash via `MessageDigest.isEqual()`
- Progressive throttling — 5 fails → 30s cooldown, doubles per fail; 15-fail optional wipe trigger (wired, no wipe)
- Hilt bindings — AuthRepository → AuthRepositoryImpl, use cases, AuthViewModel

### Out of Scope
- Biometric authentication
- Duress PIN UI and logic
- Secret gesture detector (hidden-entry change)
- Navigation graph (navigation change)
- Vault database creation (vault-db change)
- Auto-lock wiring to session (session-integration change)
- Panic wipe execution

## Capabilities

### New
- auth-pin: PIN entry, key derivation, verification, session management, throttling

### Modified
- crypto-module: ArgonKeyDeriver output changes from 32 to 64 bytes

## Approach

1. **ArgonKeyDeriver**: Increase `KEY_LENGTH_BYTES` from 32 to 64. BLAKE2b counter mode produces independent blocks — safe.
2. **AuthRepository**: Interface + `AuthRepositoryImpl` via EncryptedSharedPreferences (androidx.security:security-crypto). Keys: `salt`, `verification_hash`, `fail_count`.
3. **First-time setup**: generate 16-byte salt → derive PIN 64 bytes → SHA-256(full 64) = verification hash → store salt + hash → split [0..31]=VEK, [32..63]=DBK → openSession(VEK).
4. **Returning user**: read salt → derive PIN → SHA-256 verify → split → open session.
5. **Throttle**: increment `fail_count` on mismatch. At ≥5: cooldown = 30 × 2^(fail_count - 5) seconds, max 10 min. At ≥15: emit `PanicWipeTriggered` event (no wipe action).
6. **Constant-time**: `MessageDigest.isEqual()` for all hash comparisons.
7. **PinPad**: 3×3 grid + 0 + backspace row. Dots grow as user types (no empty circles). Wrong PIN: shake animation → clear.

## Affected Areas

| Area | Impact |
|------|--------|
| `PUhr/core/crypto/ArgonKeyDeriver.kt` | Modified — 32→64 byte output |
| `PUhr/domain/repository/AuthRepository.kt` | New — interface |
| `PUhr/domain/usecase/auth/UnlockVaultUseCase.kt` | New |
| `PUhr/domain/usecase/auth/ChangePinUseCase.kt` | New |
| `PUhr/data/repository/AuthRepositoryImpl.kt` | New — EncryptedSharedPreferences |
| `PUhr/presentation/auth/AuthScreen.kt` | New |
| `PUhr/presentation/auth/AuthViewModel.kt` | New |
| `PUhr/presentation/auth/components/PinPad.kt` | New |
| `PUhr/di/CryptoModule.kt` | Modified — add auth bindings |
| `app/build.gradle.kts` | Modified — add `security-crypto` dep |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Argon2id 64MB on low-end API 28–29 (>2-5s) | Medium | Acceptable per blueprint. Consider API-gated memory reduction if needed. |
| EncryptedSharedPreferences cleared = vault lost | Low | Inherent — salt loss = irreversible. Document as expected behavior. |
| PinPad leaks digit count or logs | Low | Custom composable, no system keyboard. Dots only, no empty slot indicators. |

## Rollback Plan

- **Data reset**: delete EncryptedSharedPreferences → app resets to first-time setup state (vault content unrecoverable).
- **Code revert**: set `KEY_LENGTH_BYTES` back to 32, remove auth-layer files → restore pre-change boundaries.
- No DB migration needed — DBK not yet consumed by vault.

## Dependencies

- `androidx.security:security-crypto:1.1.0-alpha06+` (EncryptedSharedPreferences, API 23+)

## Success Criteria

- [ ] Fresh install: user sets PIN → VEK opens session + DBK returned
- [ ] Returning: correct PIN verifies and opens session; wrong PIN increments counter
- [ ] At 5 fails: 30s lock. At 7 fails: 120s lock.
- [ ] PinPad renders correctly, no system keyboard, dots animated, shake on error
- [ ] Hilt compiles all new bindings
- [ ] Existing ArgonKeyDeriver tests pass with 64-byte output
