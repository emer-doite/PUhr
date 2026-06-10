# Exploration: Android Project Scaffold

## Current State

The project at `/home/emersong/projects/uhr` contains only `plam.md` (1278-line blueprint), `.atl/` and `openspec/` directories. No Android project structure exists — no Gradle, no manifest, no source code.

## What Needs to Be Created

### Minimum Viable Scaffold (14 files for compilation)

1. `gradle/wrapper/gradle-wrapper.properties` + wrapper JAR
2. `settings.gradle.kts`
3. Root `build.gradle.kts`
4. `gradle.properties`
5. `gradle/libs.versions.toml` (all dependencies)
6. `app/build.gradle.kts`
7. `app/src/main/AndroidManifest.xml`
8. `app/src/main/java/com/phantomvault/PhantomVaultApp.kt` (`@HiltAndroidApp`)
9. `app/src/main/java/com/phantomvault/MainActivity.kt` (`@AndroidEntryPoint` with `setContent {}`)
10. `app/src/main/res/values/themes.xml` (Material 3 dark theme)
11. `app/src/main/res/values/strings.xml` (app_name = "Timely")
12. `app/proguard-rules.pro`
13. `app/src/main/res/xml/network_security_config.xml`
14. `app/src/main/res/xml/data_extraction_rules.xml`

### Full Package Structure (~90 files total)

Under `app/src/main/java/com/phantomvault/`:
- `di/` — 4 Hilt modules
- `core/crypto/` — 5 files (AES, Argon2, Keystore, SecureWipe, Salt)
- `core/session/` — 2 files (SessionManager, AutoLockTimer)
- `core/util/` — 2 files
- `data/db/` — 5 files (Database, 2 DAOs, 2 Entities)
- `data/storage/` — 2 files (EncryptedFileStore, ThumbnailCache)
- `data/repository/` — 2 implementations
- `domain/model/` — 3 files
- `domain/repository/` — 2 interfaces
- `domain/usecase/vault/` — 7 files
- `domain/usecase/auth/` — 3 files
- `presentation/clock/` — 6 files (Screen, VM, 3 components, gesture detector)
- `presentation/auth/` — 5 files (Screen, VM, 2 components)
- `presentation/vault/` — 10 files (5 screens, 5 components)

### Resources
- `values/` — colors.xml, strings.xml, themes.xml
- `drawable/` — ic_launcher_clock.xml
- `xml/` — data_extraction_rules.xml, network_security_config.xml
- `font/` — Space Grotesk, JetBrains Mono, Inter

## Key Dependencies (from libs.versions.toml)

| Concern | Library | Version |
|---|---|---|
| Kotlin | kotlin | 2.0.0 |
| Compose BOM | compose-bom | 2024.05.00 |
| Hilt | hilt | 2.51 |
| Room | room | 2.6.1 |
| SQLCipher | sqlcipher | 4.5.6 |
| Argon2 | argon2kt | 2.0.0 |
| Coil 3 | coil | 3.0.0 |
| Biometric | biometric | 1.2.0-alpha05 |
| Coroutines | coroutines | 1.8.1 |
| AGP | agp | 8.4.0 (inferred) |
| KSP | ksp | 2.0.0-1.0.22 (inferred) |
| Navigation | navigation | 2.7.7 (inferred) |
| Lifecycle | lifecycle | 2.8.0 (inferred) |

## Risks

| Risk | Impact | Mitigation |
|---|---|---|
| No Android SDK on dev machine | Cannot validate compilation | Scaffold blind — mark NEEDS_SDK_VALIDATION |
| Compose BOM 2024.05.00 outdated for 2026 | May not compile | Update BOM during validation |
| AGP + Kotlin + KSP version compatibility | Build failures | Pin versions, verify during validation |
| SQLCipher + Room integration | Requires SupportFactory config | Include in DatabaseModule |

## Recommendation

Build the scaffold in two phases:
1. **Minimum compilation** (14 files) — validates Gradle + build system works
2. **Full package structure** — all stub files with proper declarations

## Ready for Proposal

Yes. Blueprint contains all information needed for the SDD change proposal.
