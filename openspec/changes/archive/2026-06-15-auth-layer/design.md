# Design: Auth Layer — PIN Authentication

## Technical Approach

PIN-based vault auth via Argon2id 64-byte output → SHA-256 verification → EncryptedSharedPreferences for salt/hash/throttle state. Custom Compose PinPad with no system keyboard. Progressive throttle: 5 fails → 30s, doubles. Hilt-wired AuthViewModel with sealed UiState.

## Architecture Decisions

### Decision: 64-byte Argon2id (not HKDF expansion)
- **Choice**: Increase `KEY_LENGTH_BYTES` 32→64 for direct VEK+DBK split
- **Alternatives**: Keep 32B + HKDF-Expand for domain separation (Approach 2)
- **Rationale**: Simpler single KDF call; BLAKE2b counter mode already produces independent blocks; 64MB memory cost unchanged. HKDF adds a crypto primitive, tests, and audit surface with no security benefit here.

### Decision: EncryptedSharedPreferences (not Room, not DataStore)
- **Choice**: `androidx.security:security-crypto` EncryptedSharedPreferences
- **Alternatives**: Room/SQLCipher (chicken-and-egg — need DBK to open DB to get key material), DataStore (no built-in encryption)
- **Rationale**: EncryptedSharedPreferences is AES-256 Keystore-backed, API 23+, zero-dependency for auth metadata. Salt+hash+counters are pure key-value; no relational model needed.

### Decision: SHA-256 of full 64B as verification hash
- **Choice**: `SHA-256(derive(pin, salt)[0..63])`, compared via `MessageDigest.isEqual()`
- **Alternatives**: Store VEK directly (exposes wrapped key on disk), store HKDF-derived auth-only hash
- **Rationale**: One-way hash prevents VEK recovery from stored data; 32-byte hash is standard comparison size. `MessageDigest.isEqual()` is constant-time.

### Decision: Dots-grow PinPad (not fixed-length slots)
- **Choice**: No empty slot indicators — dots appear one-per-digit, no max-length cue
- **Alternatives**: 6-12 fixed circles (leaks PIN length), single-line obscured text (feels generic)
- **Rationale**: Blueprint section 7.2: no hint of digit count. Dots-grow + shake-on-error without revealing failure location.

### Decision: Exponential throttle via fail_count (not wall-clock timers)
- **Choice**: `cooldown = 30 × 2^(fail_count - 5)` seconds for `fail_count ≥ 5`, cap 600s. At 15 → emit `PanicWipeTriggered`.
- **Alternatives**: Fixed 30s after any fail (too aggressive for 1 fail), sliding window (complex, no tangible benefit)
- **Rationale**: Exponential backoff after meaningful threshold (5). Users rarely fail 5+ times legitimately. Cap prevents indefinite lock.

## Data Flow

```ascii
PinPad ──pin──→ AuthViewModel ──pin──→ AuthRepository
                                          │
                               ┌──────────┼──────────┐
                               ▼          ▼          ▼
                          SaltGenerator  ArgonKeyDeriver  EncryptedPrefs
                               │          (64B)           (salt, hash,
                               │          │                fail_count)
                               ▼          ▼
                           SHA-256 ── MessageDigest
                           (verify)     .isEqual()
                               │
                          ┌────┴────┐
                          ▼         ▼
                     SessionMgr  Return DBK
                     .openSession(VEK)
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `PUhr/core/crypto/ArgonKeyDeriver.kt` | Modify | `KEY_LENGTH_BYTES` 32→64 (DELTA REQ-CRYPTO-02) |
| `PUhr/domain/repository/AuthRepository.kt` | Create | Interface: setup, unlock, changePin, throttle |
| `PUhr/data/repository/AuthRepositoryImpl.kt` | Create | EncryptedSharedPreferences impl |
| `PUhr/domain/usecase/auth/UnlockVaultUseCase.kt` | Create | PIN → derive → verify → split → openSession |
| `PUhr/domain/usecase/auth/ChangePinUseCase.kt` | Create | Re-derive, re-store salt+hash |
| `PUhr/presentation/auth/AuthViewModel.kt` | Create | @HiltViewModel, sealed UiState |
| `PUhr/presentation/auth/AuthScreen.kt` | Create | Compose screen, dark theme `#0A0A0C` |
| `PUhr/presentation/auth/components/PinPad.kt` | Create | 3×3 grid + 0 + backspace, dots-grow, shake |
| `PUhr/di/AuthModule.kt` | Create | Hilt @Module for AuthRepository, use cases |
| `gradle/libs.versions.toml` | Modify | Add `security-crypto` version |
| `app/build.gradle.kts` | Modify | Add `security-crypto` dependency |

## Interfaces / Contracts

```kotlin
interface AuthRepository {
    suspend fun isFirstTimeSetup(): Boolean
    suspend fun setup(pin: String): SetupResult  // salt, derive, hash, store, session
    suspend fun unlock(pin: String): UnlockResult // verify + session or fail
    suspend fun changePin(oldPin: String, newPin: String): Result<Unit>
    suspend fun remainingCooldown(): Duration
    suspend fun failCount(): Int
}

sealed class AuthUiState {
    data object Loading : AuthUiState()
    data object FirstTimeSetup : AuthUiState()
    data class PinEntry(val dotsCount: Int = 0) : AuthUiState()
    data object Authenticating : AuthUiState()
    data class Verified(val dbk: ByteArray) : AuthUiState()
    data class Throttled(val remainingSeconds: Int) : AuthUiState()
    data class Error(val isPinMismatch: Boolean = false) : AuthUiState()
}
```

## Testing Strategy

| Layer | What | Approach |
|-------|------|----------|
| Unit | ArgonKeyDeriver 64B output | Assert output length, VEK/DBK split boundaries, same input→same output |
| Unit | AuthRepository impl | Mock EncryptedSharedPreferences; verify CRUD for salt/hash/fail_count |
| Unit | Throttle logic | Test cooldown formula: 5 fails→30s, 6→60s, 7→120s, cap at 600s, 15→PanicWipeTriggered |
| Unit | AuthViewModel | Test state transitions: idle→authenticating→verified, throttle boundary |
| Compose | PinPad | Semantics tree: dot count matches input, no empty slots, shake animation fires on error |
| Integration | Full auth flow | Real Argon2id + EncryptedPrefs; verify setup→close→reopen with same PIN succeeds |

## Open Questions

- None — all decisions resolved in spec and proposal.
