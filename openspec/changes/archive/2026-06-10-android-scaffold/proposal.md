# Proposal: Android Project Scaffold

## Intent
Set up the Android project skeleton for PUhr — a disguised clock app (public name "Timely") that hides an encrypted file vault. This foundational change enables all future development.

## Scope

### In Scope
- Gradle build system (wrapper properties, version catalog, root + app build files, gradle.properties)
- `AndroidManifest.xml` with clock-themed permissions, no-network policy, backup disabled
- `@HiltAndroidApp` Application class (`PhantomVaultApp`)
- `@AndroidEntryPoint` MainActivity with Compose `setContent`
- Material 3 dark theme using blueprint design tokens (Background `#0A0A0C`, Primary `#C8A96E`, etc.)
- ProGuard rules for release builds (Room, Hilt, Argon2, SQLCipher keep rules)
- Security XML configs (`network_security_config`, `data_extraction_rules`)
- Empty `app/src/test/` and `app/src/androidTest/` directories
- Package name: `PUhr` (min API 28)

### Out of Scope
- Full package structure (domain/data/presentation layers) — deferred
- Any business logic, crypto, vault, or clock features
- DI module wiring, Room database, or file operations
- Google Fonts typefaces — typography uses Material 3 system defaults; fonts deferred

## Approach
Build the 14-file minimum scaffold for `./gradlew assembleDebug` to succeed. Build files reference `libs.versions.toml` for dependency management. Versions pinned from blueprint Appendix B (Kotlin 2.0.0, Compose BOM 2024.05.00, Hilt 2.51, AGP 8.4.0). See exploration at `openspec/changes/android-scaffold/exploration.md`.

## Capabilities

### New
- `android-scaffold`: Foundational Android project structure — build system, manifest, theme, and entry points.

### Modified
- None.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `gradle/libs.versions.toml` | New | Version catalog |
| `settings.gradle.kts` | New | Project settings |
| `build.gradle.kts` (root) | New | Root build config |
| `app/build.gradle.kts` | New | App module build config |
| `gradle.properties` | New | JVM/Gradle properties |
| `gradle/wrapper/gradle-wrapper.properties` | New | Wrapper config |
| `app/src/main/AndroidManifest.xml` | New | App manifest |
| `app/src/main/java/PUhr/PhantomVaultApp.kt` | New | `@HiltAndroidApp` |
| `app/src/main/java/PUhr/MainActivity.kt` | New | `@AndroidEntryPoint` + Compose |
| `app/src/main/res/values/themes.xml` | New | Material 3 dark theme |
| `app/src/main/res/values/strings.xml` | New | App strings |
| `app/src/main/res/xml/network_security_config.xml` | New | Network config |
| `app/src/main/res/xml/data_extraction_rules.xml` | New | Backup rules |
| `app/proguard-rules.pro` | New | ProGuard rules |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| No Android SDK on dev machine | High | Mark `NEEDS_SDK_VALIDATION` |
| Compose BOM outdated for 2026 | Medium | Update during validation |
| AGP + Kotlin + KSP version compatibility | Medium | Pin known-compatible versions |

## Rollback Plan
Delete all created files — reverts to pre-scaffold state (only `plam.md`, `.atl/`, `openspec/`).

## Dependencies
- Android SDK installed on build machine
- Gradle wrapper JAR (downloaded via `gradle wrapper`)

## Success Criteria
- [ ] `./gradlew assembleDebug` succeeds
- [ ] APK at `app/build/outputs/apk/debug/app-debug.apk`
- [ ] App launches on device/emulator showing blank Compose content with dark theme
