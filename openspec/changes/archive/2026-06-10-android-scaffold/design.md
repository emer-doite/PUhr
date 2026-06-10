# Design: Android Project Scaffold

## Technical Approach

Minimum-viable Android project scaffold — 14 files enabling `./gradlew assembleDebug`. Version catalog (`gradle/libs.versions.toml`) for dependency management. Hilt + Compose ready at the entry point. Package name `PUhr` (no reverse domain) to avoid revealing the app's purpose via package inspection. Theme uses Material 3 dark with blueprint design tokens.

## Architecture Decisions

### Decision: Gradle Plugin Application Order
**Choice**: Declare plugins in `settings.gradle.kts` via `pluginManagement`, apply in `app/build.gradle.kts`
**Alternatives considered**: Legacy `buildscript` block, all-in-root `build.gradle.kts`
**Rationale**: Version catalog + settings plugin management is the modern AGP approach. Cleaner separation of concerns.

### Decision: Package Name PUhr
**Choice**: `PUhr` (no reverse domain)
**Rationale**: Short, memorable, distinct from `com.phantomvault` which would reveal the app's purpose if decompiled.

### Decision: Theme Strategy (XML + Compose)
**Choice**: Define colors in XML `values/themes.xml`, reference from Compose via `MaterialTheme(colorScheme = darkColorScheme(...))`
**Alternatives considered**: Compose-only theme, MaterialThemeBuilder
**Rationale**: XML theme needed for the system splash screen (`windowBackground`). Compose API for runtime flexibility. Both coexist.

### Decision: Build Variants
**Choice**: `debug` (minify=false, debuggable) and `release` (R8 full mode, debuggable=false)
**Rationale**: Standard Android split. Debug enables dev tooling; release maximizes security.

### Decision: Gradle Wrapper Distribution
**Choice**: `gradle-wrapper.properties` points to Gradle 8.6
**Rationale**: Compatible with AGP 8.4.x and Kotlin 2.0.0. Latest stable at blueprint time.

### Decision: Fonts Deferred to System Defaults
**Choice**: Material 3 default typography (Roboto)
**Alternatives considered**: Google Fonts dependency, bundled TTF
**Rationale**: Keeping minimum files for compilation. Font swapping is a zero-risk cosmetic change later.

### Decision: KSP for Annotation Processing
**Choice**: KSP (Kotlin Symbol Processing) for Room and Hilt annotation processing
**Rationale**: KSP is the recommended replacement for kapt. Faster builds, native Kotlin support, compatible with both Hilt and Room.

## File Changes

| # | Path | Action | Description |
|---|------|--------|-------------|
| 1 | `gradle/wrapper/gradle-wrapper.properties` | NEW | Gradle 8.6 distribution URL |
| 2 | `settings.gradle.kts` | NEW | Plugin management, repository config, `include(":app")` |
| 3 | `build.gradle.kts` (root) | NEW | Plugin declarations (no apply) |
| 4 | `gradle.properties` | NEW | AndroidX opt-in, Kotlin code style, JVM args |
| 5 | `gradle/libs.versions.toml` | NEW | Version catalog with ALL dependencies |
| 6 | `app/build.gradle.kts` | NEW | Apply plugins, androidConfig, buildTypes, dependencies |
| 7 | `app/src/main/AndroidManifest.xml` | NEW | Package, permissions, application attributes, MainActivity |
| 8 | `app/src/main/java/PUhr/PhantomVaultApp.kt` | NEW | `@HiltAndroidApp` Application class |
| 9 | `app/src/main/java/PUhr/MainActivity.kt` | NEW | `@AndroidEntryPoint`, `setContent { MaterialTheme { } }` |
| 10 | `app/src/main/res/values/themes.xml` | NEW | Material 3 dark theme with blueprint tokens |
| 11 | `app/src/main/res/values/strings.xml` | NEW | `app_name="Timely"` |
| 12 | `app/proguard-rules.pro` | NEW | Keep rules for Room, Hilt, Argon2, SQLCipher |
| 13 | `app/src/main/res/xml/network_security_config.xml` | NEW | `cleartextTrafficPermitted=false` |
| 14 | `app/src/main/res/xml/data_extraction_rules.xml` | NEW | Exclude all from backup |
| 15 | `app/src/test/java/PUhr/` | NEW | Empty unit test directory (placeholder) |
| 16 | `app/src/androidTest/java/PUhr/` | NEW | Empty instrumented test directory (placeholder) |

## Build Configuration

| Property | Value |
|----------|-------|
| Gradle | 8.6 |
| AGP | 8.4.0 |
| Kotlin | 2.0.0 |
| KSP | 2.0.0-1.0.22 |
| compileSdk | 34 |
| minSdk | 28 |
| targetSdk | 34 |
| Kotlin JVM target | 17 |
| Compose BOM | 2024.05.00 |
| R8 | Full mode (release) |

### Plugin Declarations (`settings.gradle.kts`)
```kotlin
plugins {
    id("com.android.application") version "8.4.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
    id("com.google.devtools.ksp") version "2.0.0-1.0.22" apply false
    id("com.google.dagger.hilt.android") version "2.51" apply false
}
```

### androidConfig (`app/build.gradle.kts`)
```kotlin
android {
    namespace = "PUhr"
    compileSdk = 34

    defaultConfig {
        applicationId = "PUhr"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}
```

## Interfaces / Contracts

### Version Catalog (`gradle/libs.versions.toml`)

**Versions:**
```toml
[versions]
agp = "8.4.0"
kotlin = "2.0.0"
ksp = "2.0.0-1.0.22"
compose-bom = "2024.05.00"
hilt = "2.51"
room = "2.6.1"
sqlcipher = "4.5.6"
argon2 = "2.0.0"
coil = "3.0.0"
biometric = "1.2.0-alpha05"
coroutines = "1.8.1"
navigation = "2.7.7"
lifecycle = "2.8.0"
activity-compose = "1.9.0"
core-ktx = "1.13.1"
```

**Libraries:**
| Alias | Maven coordinate |
|-------|-----------------|
| `hilt-android` | `com.google.dagger:hilt-android:2.51` |
| `hilt-compiler` | `com.google.dagger:hilt-compiler:2.51` |
| `hilt-navigation-compose` | `androidx.hilt:hilt-navigation-compose:1.2.0` |
| `room-runtime` | `androidx.room:room-runtime:2.6.1` |
| `room-ktx` | `androidx.room:room-ktx:2.6.1` |
| `room-compiler` | `androidx.room:room-compiler:2.6.1` |
| `sqlcipher` | `net.zetetic:android-database-sqlcipher:4.5.6` |
| `argon2` | `com.lambdapioneer.argon2kt:argon2kt:2.0.0` |
| `coil-compose` | `io.coil-kt.coil3:coil-compose:3.0.0` |
| `biometric` | `androidx.biometric:biometric:1.2.0-alpha05` |
| `navigation-compose` | `androidx.navigation:navigation-compose:2.7.7` |
| `lifecycle-runtime-ktx` | `androidx.lifecycle:lifecycle-runtime-ktx:2.8.0` |
| `lifecycle-viewmodel-compose` | `androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0` |
| `lifecycle-runtime-compose` | `androidx.lifecycle:lifecycle-runtime-compose:2.8.0` |
| `activity-compose` | `androidx.activity:activity-compose:1.9.0` |
| `core-ktx` | `androidx.core:core-ktx:1.13.1` |
| `coroutines-android` | `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1` |

**Plugins** (all reference by alias):
- `android-application` → `com.android.application` version ref `agp`
- `kotlin-android` → `org.jetbrains.kotlin.android` version ref `kotlin`
- `kotlin-compose` → `org.jetbrains.kotlin.plugin.compose` version ref `kotlin`
- `hilt` → `com.google.dagger.hilt.android` version ref `hilt`
- `ksp` → `com.google.devtools.ksp` version ref `ksp`

**Bundles:**
- `compose-bom` → `androidx.compose:compose-bom:2024.05.00`
- `compose-ui` (from BOM): `androidx.compose.ui:ui`, `androidx.compose.ui:ui-graphics`, `androidx.compose.ui:ui-tooling-preview`
- `compose-material3` (from BOM): `androidx.compose.material3:material3`

## Manifest Attributes

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="PUhr">

    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.USE_BIOMETRIC" />
    <uses-permission android:name="android.permission.USE_FINGERPRINT" />

    <application
        android:name=".PhantomVaultApp"
        android:allowBackup="false"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:networkSecurityConfig="@xml/network_security_config"
        android:label="Timely"
        android:supportsRtl="true"
        android:theme="@style/Theme.PhantomVault">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.PhantomVault">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

## Theme Tokens (from plam.md §11.1)

| Token | Color |
|-------|-------|
| Background | `#0A0A0C` |
| Surface | `#141416` |
| Surface Variant | `#1E1E22` |
| Primary | `#C8A96E` |
| Secondary | `#6E8DC8` |
| Error | `#CF6679` |
| Text Primary | `#F2EDE4` |
| Text Secondary | `#8A8A94` |

## Testing Strategy

No tests for this change. Empty `app/src/test/java/PUhr/` and `app/src/androidTest/java/PUhr/` directories are created as placeholders. Strict TDD is disabled until the build system is operational and the first unit-testable module (crypto) lands.

## Migration / Rollout

No migration required. This is the first code in the repo. Rollback deletes all 14 files, reverting to the pre-scaffold state (only `plam.md`, `.atl/`, `openspec/`).

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| No Android SDK on dev machine | High | Mark `NEEDS_SDK_VALIDATION` on completion |
| Compose BOM 2024.05.00 outdated for 2026 | Medium | Update BOM version during validation |
| AGP + Kotlin + KSP version compatibility | Medium | Versions pinned from known-compatible matrix |
| Gradle wrapper JAR missing | High | Must run `gradle wrapper --gradle-version 8.6` or download manually |
