# Auth PIN Specification

## Purpose

PIN-based vault authentication: Argon2id key derivation (64-byte), VEK+DBK split, SHA-256 verification, progressive throttling, and session lifecycle. Covers first-time setup, returning user unlock, and the custom PIN pad UI.

## Requirements

### Requirement: PIN Entry UI (REQ-AUTH-01)

The PIN pad MUST be a custom Compose composable with no system keyboard. Dots MUST grow as digits are entered — no empty slot hints. Wrong PIN MUST trigger a shake animation then clear silently with no error text.

#### Scenario: Incremental dot feedback
- GIVEN AuthScreen is displayed
- WHEN the user taps digits
- THEN a dot appears per digit, no count limit is visible

#### Scenario: No empty slot hints
- GIVEN AuthScreen is displayed
- WHEN no digits have been entered
- THEN zero dots are shown — no placeholder circles or lines

#### Scenario: Wrong PIN clears with shake
- GIVEN the user entered a PIN
- WHEN verification fails
- THEN the dots shake once, then clear silently with no message

### Requirement: Key Derivation (REQ-AUTH-02)

The system MUST derive 64 bytes from the PIN and a 16-byte salt using Argon2id (m=65536KB, t=3, p=4). Bytes [0..31] MUST be VEK; bytes [32..63] MUST be DBK.

#### Scenario: Output is exactly 64 bytes
- GIVEN a PIN and salt
- WHEN derive runs
- THEN the result is 64 bytes

#### Scenario: VEK+DBK split
- GIVEN a 64-byte derived output
- WHEN bytes [0..31] and [32..63] are extracted
- THEN first 32 bytes are VEK, last 32 bytes are DBK, both non-null

### Requirement: PIN Verification (REQ-AUTH-03)

The system MUST compute SHA-256 of the full 64-byte derived key and compare against the stored hash via MessageDigest.isEqual() (constant-time).

#### Scenario: Correct PIN matches
- GIVEN a stored verification hash for a known PIN
- WHEN the correct PIN is entered and derived
- THEN isEqual returns true

#### Scenario: Wrong PIN fails
- GIVEN a stored verification hash
- WHEN an incorrect PIN is entered
- THEN isEqual returns false and fail count increments

### Requirement: First-Time Vault Setup (REQ-AUTH-04)

On first launch, the system MUST generate a 16-byte salt, derive 64 bytes from the user's chosen PIN, store salt+SHA-256 hash via AuthRepository, split to VEK+DBK, and open a session with VEK.

#### Scenario: Fresh install setup
- GIVEN no stored salt exists
- WHEN the user sets a PIN
- THEN salt is generated, keys derived, hash stored, session opened with VEK

#### Scenario: Setup persists
- GIVEN a completed first-time setup
- WHEN the app restarts
- THEN salt and verification hash are retrievable from AuthRepository

### Requirement: Returning User Unlock (REQ-AUTH-05)

The system MUST retrieve stored salt, derive the entered PIN, verify against stored hash, split to VEK+DBK, and open session with VEK.

#### Scenario: Correct PIN unlocks
- GIVEN stored salt and hash from prior setup
- WHEN the correct PIN is entered
- THEN session opens with VEK and DBK is returned

#### Scenario: No stored salt redirects to setup
- GIVEN no salt or hash stored
- WHEN any PIN is entered
- THEN system redirects to first-time setup flow

### Requirement: Progressive Throttling (REQ-AUTH-06)

After 5 consecutive failures the system MUST enforce a cooldown of 30 seconds, doubling each additional failure (60s, 120s, 240s) to a 10-minute maximum. At 15 failures the system MUST emit PanicWipeTriggered.

#### Scenario: 30-second cooldown at 5 failures
- GIVEN 5 consecutive failed attempts
- WHEN a 6th attempt is made
- THEN a 30-second cooldown is enforced

#### Scenario: Cooldown doubles
- GIVEN 7 consecutive failed attempts
- WHEN an 8th attempt is made
- THEN a 120-second cooldown is enforced

#### Scenario: Panic trigger at 15 failures
- GIVEN 15 consecutive failed attempts
- WHEN threshold is reached
- THEN PanicWipeTriggered event is emitted

### Requirement: Auth State Management (REQ-AUTH-07)

AuthViewModel MUST expose a sealed UiState: PinEntry | Authenticating | Verified | Error | Throttled | FirstTimeSetup. AuthRepository MUST persist salt, verification hash, and fail count via EncryptedSharedPreferences. All wired via Hilt.

#### Scenario: Submitting transitions to Authenticating
- GIVEN AuthViewModel in PinEntry state
- WHEN user submits a PIN
- THEN state becomes Authenticating

#### Scenario: Success transitions to Verified
- GIVEN AuthViewModel in Authenticating state
- WHEN verification succeeds
- THEN state becomes Verified

#### Scenario: Throttled state at cooldown
- GIVEN AuthViewModel fail count ≥ 5
- WHEN user attempts to enter a PIN
- THEN state becomes Throttled showing remaining cooldown seconds
