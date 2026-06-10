# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [0.1.0] — 2026-06-10

### Added
- Project blueprint (`plam.md`) — full design & development blueprint for PhantomVault (Timely)
- SDD initialization: `openspec/` directory structure, `openspec/config.yaml`, project context
- Skill registry: `.atl/skill-registry.md` with available skills indexed
- `openspec/changes/android-scaffold/exploration.md` — analysis of Android project scaffold requirements
- `openspec/changes/android-scaffold/proposal.md` — formal SDD proposal for the first change
- `.gitignore` for Android/Kotlin project
- `VERSION` file (semver 0.1.0)
- `CHANGELOG.md` (this file)

### Added — Android Scaffold
- Gradle 8.6 wrapper with AGP 8.4.0, Kotlin 2.0.0, KSP 2.0.0-1.0.22
- Version catalog (`gradle/libs.versions.toml`) with all dependencies
- Root `build.gradle.kts` and `settings.gradle.kts` with plugin management
- `gradle.properties` with AndroidX opt-in, non-transitive R classes
- `app/build.gradle.kts` with compose, Hilt, Room, SQLCipher, Argon2, Coil, Biometric, Navigation
- `AndroidManifest.xml` with PUhr package, permissions, security config
- `PhantomVaultApp.kt` — `@HiltAndroidApp` Application class
- `MainActivity.kt` — `@AndroidEntryPoint` with Material3 dark Compose theme
- `themes.xml` — Material3 Dark.NoActionBar with blueprint color tokens
- `strings.xml` — `app_name="Timely"`
- `network_security_config.xml` — cleartext traffic disabled
- `data_extraction_rules.xml` — backup excluded
- `proguard-rules.pro` — keep rules for Room, Hilt, Argon2, SQLCipher
- Empty test directories (`app/src/test/java/PUhr/`, `app/src/androidTest/java/PUhr/`)
- `com.google.android.material:material:1.12.0` for Material3 XML theme support
