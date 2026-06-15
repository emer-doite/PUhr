# Delta for Crypto Module

## MODIFIED Requirements

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
