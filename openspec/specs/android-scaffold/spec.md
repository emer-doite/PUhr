# Android Scaffold Specification

## Purpose

Define the foundational Android project skeleton for PUhr — a disguised clock app (public name "Timely") that hides an encrypted file vault. This scaffold provides the build system, manifest, theme, and entry points for a Clock + Vault app. Compilation of `./gradlew assembleDebug` is the minimum viability gate.

## Requirements

### Requirement: Build System

The project MUST compile with Gradle 8.x and AGP 8.4.x.
The project SHALL use a version catalog at `gradle/libs.versions.toml` for dependency management.

#### Scenario: Version catalog declares all dependencies

- GIVEN the project has a `gradle/libs.versions.toml` file
- THEN it MUST declare versions for kotlin (2.0.0), agp, compose-bom (2024.05.00), hilt (2.51), room (2.6.1), sqlcipher (4.5.6), argon2 (2.0.0), coil (3.0.0), biometric (1.2.0-alpha05), coroutines (1.8.1), ksp (2.0.0-1.0.22), navigation (2.7.7), lifecycle (2.8.0)
- AND each plugin (android-application, kotlin-android, kotlin-compose, hilt, ksp) MUST reference a version from the catalog

#### Scenario: assembleDebug produces an APK

- GIVEN the project build configuration
- WHEN `./gradlew assembleDebug` is executed
- THEN it MUST succeed without errors
- AND produce `app/build/outputs/apk/debug/app-debug.apk`

### Requirement: AndroidManifest

The manifest MUST declare the PUhr package with minSdkVersion=28. The application MUST have allowBackup=false and debuggable=false for release builds.

#### Scenario: Manifest declares minimal permissions

- GIVEN the AndroidManifest.xml
- THEN it MUST declare: RECEIVE_BOOT_COMPLETED, SCHEDULE_EXACT_ALARM, USE_BIOMETRIC, USE_FINGERPRINT
- AND it MUST NOT declare INTERNET, ACCESS_NETWORK_STATE, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, or CAMERA

### Requirement: Application and Entry Point

The app MUST have an @HiltAndroidApp Application class and an @AndroidEntryPoint Activity.

#### Scenario: App class annotated with Hilt

- GIVEN the Application class PhantomVaultApp
- THEN it MUST be annotated with @HiltAndroidApp
- AND live under the PUhr package

#### Scenario: MainActivity shows Compose content

- GIVEN the MainActivity
- WHEN it launches
- THEN it MUST call setContent with a MaterialTheme wrapper
- AND use the blueprint dark color scheme

### Requirement: Theme

The app SHALL use a Material 3 dark theme with the design tokens from plam.md.

#### Scenario: Theme uses blueprint colors

- GIVEN the themes.xml
- THEN the background color MUST be #0A0A0C
- AND the primary color MUST be #C8A96E
- AND the surface color MUST be #141416
- AND the text primary color MUST be #F2EDE4

### Requirement: Security Configuration

The app MUST have network_security_config.xml blocking cleartext traffic. The app MUST have data_extraction_rules.xml excluding all sensitive data from backup.

#### Scenario: Network config blocks cleartext

- GIVEN the network_security_config.xml
- THEN it MUST set cleartextTrafficPermitted="false"

#### Scenario: Data extraction rules exclude vault

- GIVEN the data_extraction_rules.xml
- THEN it MUST exclude the vault directory from cloud backup
- AND exclude all file-based backup

### Requirement: ProGuard Rules

Release builds SHALL obfuscate with R8 full mode and keep rules for Room entities, Hilt, Argon2 JNI, and SQLCipher.

#### Scenario: ProGuard keeps required classes

- GIVEN the proguard-rules.pro file
- THEN it MUST include -keep rules for: com.PUhr.data.db.entity.**, dagger.hilt.**, com.lambdapioneer.argon2kt.**, net.sqlcipher.**

### Requirement: Error Repair Directive

If any build error or test failure is detected during development, it MUST be resolved before adding new features or advancing to the next phase.
