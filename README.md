# PUhr

Android app disguised as a minimal clock — hides an encrypted vault beneath.

## Stack

- Kotlin, Jetpack Compose, Material3
- Hilt (DI), Room, SQLCipher
- Argon2id (key derivation), AES-256-GCM (encryption)
- Min SDK 28 / Target SDK 34

## What's built

| Layer | Status |
|-------|--------|
| Clock face (disguise) | Done |
| Crypto primitives | Done |
| Auth (PIN + throttle) | Done |
| Vault (hidden entry + storage) | Pending |

## Build

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## Structure

```
app/src/main/java/PUhr/
  auth/          Auth UI (PinPad, AuthScreen, AuthViewModel)
  clock/         Clock disguise (AnalogClockFace, ClockScreen)
  core/crypto/   KeyDeriver, AesGcmCipher, KeystoreManager, SaltGenerator
  core/session/  SessionManager, AutoLockTimer
  data/           Repository implementations
  domain/         Repository interfaces, use cases
  di/             Hilt modules
```

## Version

See [VERSION](VERSION) — [CHANGELOG](CHANGELOG.md) for history.
