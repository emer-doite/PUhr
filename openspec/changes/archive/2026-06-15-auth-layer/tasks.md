# Tasks: Auth Layer — PIN Authentication

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~800-900 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1: Foundation + Core → PR 2: UI + Tests |
| Delivery strategy | ask-on-risk |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Foundation + Core (deps, storage, repository, use cases, key deriver update) | PR 1 | base=main; includes storage/throttle tests |
| 2 | UI + ViewModel + Compose tests | PR 2 | depends on PR 1 |

## Phase 1: Foundation — Storage and DI (5 tasks)

- [x] 1.1 Add `security-crypto` version + library to `gradle/libs.versions.toml`
- [x] 1.2 Add `implementation(libs.security.crypto)` to `app/build.gradle.kts`
- [x] 1.3 Create `PUhr/domain/repository/AuthRepository.kt` — interface: `setup`, `unlock`, `changePin`, `remainingCooldown`, `failCount`, `isFirstTimeSetup`
- [x] 1.4 Create `PUhr/data/repository/AuthRepositoryImpl.kt` — EncryptedSharedPreferences CRUD; throttle `30×2^(n-5)` cap 600s; emit `PanicWipeTriggered` at 15
- [x] 1.5 Create `PUhr/di/AuthModule.kt` — Hilt `@Module` binding `AuthRepository`, provisioning use cases, `SaltGenerator`, `KeyDeriver`

## Phase 2: Core — Key Derivation and Use Cases (4 tasks)

- [x] 2.1 Set `KEY_LENGTH_BYTES=64` in `PUhr/core/crypto/ArgonKeyDeriver.kt`
- [x] 2.2 Update `FakeKeyDeriver.kt` to produce 64-byte output (SHA-512)
- [x] 2.3 Create `PUhr/domain/usecase/auth/UnlockVaultUseCase.kt` — derive→verify→VEK/DBK split→`SessionManager.openSession(VEK)`→return DBK
- [x] 2.4 Create `PUhr/domain/usecase/auth/ChangePinUseCase.kt` — verify old, re-derive new, re-store salt+hash

## Phase 3: UI — PinPad, AuthScreen, AuthViewModel (3 tasks)

- [x] 3.1 Create `PUhr/presentation/auth/components/PinPad.kt` — 3×3+0+backspace, dots-grow, shake on error, dark `#0A0A0C`
- [x] 3.2 Create `PUhr/presentation/auth/AuthScreen.kt` — wiring `AuthViewModel`→`PinPad`, sealed `AuthUiState` render
- [x] 3.3 Create `PUhr/presentation/auth/AuthViewModel.kt` — `@HiltViewModel` with `AuthUiState`, throttle, verify via use case

## Phase 4: Tests — Verify All Layers (4 tasks)

- [x] 4.1 Update `ArgonKeyDeriverInstrumentedTest.kt` — assert 64B output, VEK bytes[0..31], DBK bytes[32..63]
- [x] 4.2 Create `AuthRepositoryTest.kt` — mock prefs; verify CRUD, throttle at 5/6/7/15, PanicWipeTriggered
- [x] 4.3 Create `AuthViewModelTest.kt` — state transitions: PinEntry→Authenticating→Verified, Throttled, Error
- [x] 4.4 Create `PinPadTest.kt` (androidTest) — semantics: dot count matches input, shake on error, empty on init
