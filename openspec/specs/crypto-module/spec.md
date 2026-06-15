# Crypto Module Specification

## Purpose

Core cryptographic primitives for the PUhr vault: AES-256-GCM encryption, Argon2id key derivation, Android Keystore wrapping, secure file destruction, salt generation, session management with auto-lock, and Hilt DI wiring.

## Requirements

### Requirement: AES-256-GCM Encryption (REQ-CRYPTO-01)

The system MUST encrypt and decrypt ByteArray data with AES-256-GCM using a 256-bit key and a random 96-bit IV per operation, producing a 128-bit authentication tag.

#### Scenario: Encrypt-decrypt round-trip
- GIVEN a plaintext, a 256-bit key, and a 96-bit IV
- WHEN encrypt is called followed by decrypt with the same key and IV
- THEN the output MUST equal the original plaintext

#### Scenario: Tampered ciphertext detection
- GIVEN a valid ciphertext from encrypt
- WHEN any byte of ciphertext or auth tag is modified before decrypt
- THEN decrypt MUST throw AEADBadTagException

### Requirement: Argon2id Key Derivation (REQ-CRYPTO-02)

The system MUST derive a 64-byte key (previously 32 bytes) from a PIN and 128-bit salt using Argon2id (m=65536KB, t=3, p=4). A KeyDeriver interface MUST abstract the implementation — JVM tests use a fake; instrumented tests use real Argon2kt JNI. Bytes [0..31] are VEK, bytes [32..63] are DBK.
(Previously: 256-bit output, single key, no VEK/DBK split)

#### Scenario: Same PIN + salt produces same 64-byte key
- GIVEN a PIN and a 128-bit salt
- WHEN derive runs twice with identical inputs
- THEN both outputs are identical 64-byte keys

#### Scenario: Different salts produce different keys
- GIVEN the same PIN
- WHEN derive runs with two different salts
- THEN the resulting 64-byte keys differ

#### Scenario: VEK+DBK split correctness
- GIVEN a 64-byte derived output
- WHEN bytes [0..31] are extracted as VEK and [32..63] as DBK
- THEN both 32-byte segments are independently usable and non-null

### Requirement: Android Keystore Integration (REQ-CRYPTO-03)

The system MUST manage an AES-256/GCM/NoPadding wrapping key in Android Keystore under alias "pv_master_key_wrap". KeystoreManager MUST be a @Singleton class injectable via Hilt. setUnlockedDeviceRequired(true) MUST apply on API 30+ and MUST be skipped on API 28-29.

#### Scenario: Key generation stores wrapping key
- GIVEN Android Keystore is available
- WHEN generateWrappingKey is called
- THEN a 256-bit AES key exists under alias "pv_master_key_wrap"

#### Scenario: API 28-29 fallback
- GIVEN the device runs API 28 or 29
- WHEN generateWrappingKey is called
- THEN setUnlockedDeviceRequired is NOT applied and the key is generated

#### Scenario: Key retrieval returns initialized Cipher
- GIVEN a wrapping key in Keystore
- WHEN getCipher(PURPOSE_ENCRYPT) is called
- THEN a Cipher initialized in ENCRYPT_MODE with the wrapping key is returned

### Requirement: Secure Wipe (REQ-CRYPTO-04)

The system MUST overwrite file contents with 3 random-byte passes via NIO FileChannel + force(), then delete the file.

#### Scenario: File wiped and deleted
- GIVEN a file with known content at path P
- WHEN wipeFile(P) is called
- THEN the file's bytes are overwritten with random data and the file is deleted
- AND the file no longer exists at path P

#### Scenario: Non-existent file is no-op
- GIVEN a path with no file
- WHEN wipeFile is called
- THEN no exception is thrown

### Requirement: Salt Generation (REQ-CRYPTO-05)

The system MUST generate 128-bit (16-byte) cryptographically random salts via java.security.SecureRandom.

#### Scenario: Generates 128-bit salt
- GIVEN a SaltGenerator
- WHEN generate is called
- THEN a 16-byte ByteArray is returned

#### Scenario: Successive salts diverge
- GIVEN a SaltGenerator
- WHEN generate is called twice
- THEN the two salts MUST differ

### Requirement: Session Management (REQ-CRYPTO-06)

The system MUST hold the VEK in a ByteArray in memory only — never persisted. closeSession MUST zero-fill the array before setting it to null.

#### Scenario: Open and retrieve session key
- GIVEN a 256-bit VEK
- WHEN openSession(VEK) is called
- THEN getKey returns the VEK

#### Scenario: Close session zero-fills key
- GIVEN an open session with a VEK stored
- WHEN closeSession is called
- THEN the stored ByteArray is filled with zeros and getKey returns null

#### Scenario: Close with no active session is safe
- GIVEN no session is open
- WHEN closeSession is called
- THEN no exception is thrown

### Requirement: Auto-Lock Timer (REQ-CRYPTO-07)

The system MUST close the session after a configurable timeout (30s–5min) via a coroutine timer. Explicit closeSession MUST cancel the timer.

#### Scenario: Timer fires and locks session
- GIVEN an open session with a 1-second timeout
- WHEN 1 second elapses
- THEN closeSession is called and the VEK is zero-filled

#### Scenario: Manual close cancels timer
- GIVEN an open session with a 60-second timeout
- WHEN closeSession is called explicitly
- THEN the timer is cancelled and the session clears

### Requirement: Hilt Crypto Module (REQ-CRYPTO-08)

The system MUST provide a Hilt @Module binding all crypto deps. Stateless utilities use @Provides; stateful classes use @Inject constructor with @Singleton.

#### Scenario: All bindings compile and resolve
- GIVEN a Hilt project with CryptoModule installed
- WHEN ./gradlew assembleDebug runs
- THEN all crypto bindings resolve without DI errors

## Risks

- Argon2kt JNI unavailable on desktop JVM → KeyDeriver interface + fake for JVM tests
- setUnlockedDeviceRequired crash on API <30 → runtime SDK check
- argon2kt 1.6.0 API differs from blueprint's 2.0.0 → verify constructor at design time
- IV+key reuse breaks GCM → caller must provide unique IV per operation
