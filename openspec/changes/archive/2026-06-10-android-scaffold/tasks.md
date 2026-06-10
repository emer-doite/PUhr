# Tasks: android-scaffold

**Change**: Android Project Scaffold — 16 files for minimum viable PUhr project.
**Gate**: `./gradlew assembleDebug` succeeds, APK produced at `app/build/outputs/apk/debug/app-debug.apk`.
**Line budget**: ~313 new lines (all boilerplate/config, no logic).
**Depends on**: Android SDK on build machine, Gradle wrapper JAR.

---

## Phase 1: Gradle Build System

Core build wiring — wrapper, catalog, root scripts, properties.

### Task 1.1 — `gradle/wrapper/gradle-wrapper.properties`
- **File**: `gradle/wrapper/gradle-wrapper.properties` (NEW)
- **Content**: `distributionBase`, `distributionPath`, `distributionUrl` pointing to `gradle-8.6-bin.zip`, `zipStoreBase`, `zipStorePath`
- **Key value**: `distributionUrl=https\://services.gradle.org/distributions/gradle-8.6-bin.zip`
- **Checks**: 5 lines, verified by `./gradlew --version`

### Task 1.2 — `gradle/libs.versions.toml`
- **File**: `gradle/libs.versions.toml` (NEW)
- **Content**: Complete version catalog with all versions, libraries, plugins, and bundles from design.md
- **Versions** (13): agp, kotlin, ksp, compose-bom, hilt, room, sqlcipher, argon2, coil, biometric, coroutines, navigation, lifecycle, activity-compose, core-ktx
- **Libraries** (17 aliases): hilt-android, hilt-compiler, hilt-navigation-compose, room-runtime, room-ktx, room-compiler, sqlcipher, argon2, coil-compose, biometric, navigation-compose, lifecycle-runtime-ktx, lifecycle-viewmodel-compose, lifecycle-runtime-compose, activity-compose, core-ktx, coroutines-android
- **Plugins** (5): android-application, kotlin-android, kotlin-compose, hilt, ksp
- **Bundles**: compose-bom, compose-ui, compose-material3
- **Checks**: All versions pinned, no `++` mutations, plugins reference `version.ref` not inline strings

### Task 1.3 — Root `build.gradle.kts`
- **File**: `build.gradle.kts` (NEW, root project)
- **Content**: `plugins { }` block declaring all 5 plugins with `apply false` — no android block, no dependencies
- **Checks**: Matches design.md plugin declarations exactly

### Task 1.4 — `settings.gradle.kts`
- **File**: `settings.gradle.kts` (NEW)
- **Content**: `pluginManagement { repositories { google(), mavenCentral(), gradlePluginPortal() } }`, `dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(), mavenCentral() } }`, `rootProject.name = "PUhr"`, `include(":app")`
- **Checks**: Root project name "PUhr", `:app` included

### Task 1.5 — `gradle.properties`
- **File**: `gradle.properties` (NEW)
- **Content**: `android.useAndroidX=true`, `kotlin.code.style=official`, `android.nonTransitiveRClass=true`, JVM args (`-Xmx2048m`), `org.gradle.jvmargs=-Xmx2048m`
- **Checks**: No `android.enableJetifier=true` (not needed), KSP opt-in not required (handled by plugin)

---

## Phase 2: App Module

Module-level build, manifest, and ProGuard.

### Task 2.1 — `app/build.gradle.kts`
- **File**: `app/build.gradle.kts` (NEW)
- **Content**: Apply 5 plugins (`com.android.application`, `org.jetbrains.kotlin.android`, `org.jetbrains.kotlin.plugin.compose`, `com.google.devtools.ksp`, `com.google.dagger.hilt.android`). `androidConfig` block: `namespace="PUhr"`, `compileSdk=34`, `defaultConfig { applicationId="PUhr"; minSdk=28; targetSdk=34; versionCode=1; versionName="1.0.0" }`. `buildTypes`: `release` (minify=true, debug=false, proguard), `debug` (minify=false, debug=true, applicationIdSuffix=".debug"). `compileOptions` / `kotlinOptions` targeting JVM 17. `buildFeatures { compose=true }`. `dependencies` block referencing `libs.<catalog-alias>` for all libraries.
- **Checks**: All dependencies from catalog, no hardcoded versions, KSP annotation processor config for Room and Hilt compiler

### Task 2.2 — `app/src/main/AndroidManifest.xml`
- **File**: `app/src/main/AndroidManifest.xml` (NEW)
- **Content**: `package="PUhr"`. Permissions: RECEIVE_BOOT_COMPLETED, SCHEDULE_EXACT_ALARM, USE_BIOMETRIC, USE_FINGERPRINT. Application: `android:name=".PhantomVaultApp"`, `allowBackup=false`, `dataExtractionRules`, `networkSecurityConfig`, `label="Timely"`, `supportsRtl=true`, `theme="@style/Theme.PhantomVault"`. Activity MainActivity with `exported=true`, intent-filter MAIN/LAUNCHER.
- **Checks**: No INTERNET/ACCESS_NETWORK_STATE/STORAGE/CAMERA permissions. Package is PUhr (not com.PUhr).

### Task 2.3 — `app/proguard-rules.pro`
- **File**: `app/proguard-rules.pro` (NEW)
- **Content**: `-keep class com.PUhr.data.db.entity.** { *; }`, `-keep class dagger.hilt.** { *; }`, `-keep class com.lambdapioneer.argon2kt.** { *; }`, `-keep class net.sqlcipher.** { *; }`, `-dontwarn` for same packages. R8 full mode comment.
- **Checks**: All four keep rules present, syntax valid

---

## Phase 3: Application Code

Hilt Application and Compose Activity.

### Task 3.1 — `PhantomVaultApp.kt`
- **File**: `app/src/main/java/PUhr/PhantomVaultApp.kt` (NEW)
- **Content**: `package PUhr`, import `dagger.hilt.android.HiltAndroidApp`, `@HiltAndroidApp` annotation on `class PhantomVaultApp : Application()`
- **Checks**: No `onCreate` override unless needed, package `PUhr` (not `com.PUhr`)

### Task 3.2 — `MainActivity.kt`
- **File**: `app/src/main/java/PUhr/MainActivity.kt` (NEW)
- **Content**: `package PUhr`, imports for `androidx.activity.ComponentActivity`, `androidx.activity.compose.setContent`, `dagger.hilt.android.AndroidEntryPoint`, compose `MaterialTheme`. `@AndroidEntryPoint class MainActivity : ComponentActivity() { override fun onCreate(...) { super.onCreate(...); setContent { MaterialTheme { /* blank */ } } } }`. Dark color scheme from blueprint tokens.
- **Checks**: Blueprint dark colors match design.md theme tokens. Theme reference uses `MaterialTheme` (not custom theme). Content lambda is empty placeholder.

---

## Phase 4: Resources

XML resources: strings, theme, security configs.

### Task 4.1 — `strings.xml`
- **File**: `app/src/main/res/values/strings.xml` (NEW)
- **Content**: `<resources><string name="app_name">Timely</string></resources>`
- **Checks**: `app_name` matches manifest `android:label="Timely"`

### Task 4.2 — `themes.xml`
- **File**: `app/src/main/res/values/themes.xml` (NEW)
- **Content**: Material 3 dark theme `Theme.PhantomVault` inheriting `Theme.Material3.Dark.NoActionBar`. Override attributes for `colorBackground=#0A0A0C`, `colorPrimary=#C8A96E`, `colorSurface=#141416`, `colorOnSurface=#F2EDE4`, and other surface variant/secondary/error tokens.
- **Checks**: Background `#0A0A0C`, Primary `#C8A96E`, Surface `#141416`, Text Primary `#F2EDE4` — all match plam.md §11.1 tokens. `Theme.PhantomVault` referenced in manifest.

### Task 4.3 — `network_security_config.xml`
- **File**: `app/src/main/res/xml/network_security_config.xml` (NEW)
- **Content**: `<network-security-config><base-config cleartextTrafficPermitted="false"><trust-anchors><certificates src="system" /></trust-anchors></base-config></network-security-config>`
- **Checks**: `cleartextTrafficPermitted="false"`, well-formed XML

### Task 4.4 — `data_extraction_rules.xml`
- **File**: `app/src/main/res/xml/data_extraction_rules.xml` (NEW)
- **Content**: `<data-extraction-rules><cloud-backup><exclude domain="root" /></cloud-backup><device-transfer><exclude domain="root" /></device-transfer></data-extraction-rules>`
- **Checks**: Excludes all domains from both cloud and device-to-device backup

---

## Phase 5: Test Placeholders

Empty test directories with `.gitkeep`.

### Task 5.1 — Unit test directory
- **File**: `app/src/test/java/PUhr/.gitkeep` (NEW)
- **Content**: Empty file (`.gitkeep` marker)
- **Checks**: Directory structure created, `.gitkeep` committed

### Task 5.2 — Instrumented test directory
- **File**: `app/src/androidTest/java/PUhr/.gitkeep` (NEW)
- **Content**: Empty file (`.gitkeep` marker)
- **Checks**: Directory structure created, `.gitkeep` committed

---

## Dependency Order

```
Phase 1 ──► Phase 2 ──► Phase 3 ──► Phase 4 ──► Phase 5
(1.1-1.5)    (2.1-2.3)    (3.1-3.2)    (4.1-4.4)    (5.1-5.2)
```

- 1.1 (wrapper) must exist before Gradle can resolve anything
- 2.1 (app build) depends on 1.2 (catalog) and 1.4 (settings)
- 2.2 (manifest) depends on 4.1 (strings) and 4.2 (theme) for XML references
- 3.1-3.2 depend on 2.1, 2.2
- 5.1-5.2 are standalone; can be done any time

---

## Verification Gate

After all tasks: `./gradlew assembleDebug`

| Check | Command |
|-------|---------|
| Clean build | `./gradlew clean assembleDebug` |
| APK exists | `ls -la app/build/outputs/apk/debug/app-debug.apk` |
| No lint errors | `./gradlew lint` |
| No KSP errors | `./gradlew kspDebugKotlin` (checks annotation processing) |

### Known Risks at Gate
1. **No Android SDK** — mark `NEEDS_SDK_VALIDATION` in summary
2. **Gradle wrapper JAR missing** — must run `gradle wrapper --gradle-version 8.6` from a host with Gradle installed, or download manually
3. **BOM version stale** — if Compose BOM `2024.05.00` fails in 2026, upgrade to latest stable

---

## Review Workload Forecast

| Metric | Value |
|--------|-------|
| Files changed | 16 (14 content files + 2 .gitkeep) |
| Lines added (estimate) | ~313 |
| Lines changed (estimate) | ~313 (all new) |
| Complexity | Low — all boilerplate/config |
| Risk per review unit | Minimal — each file independently reviewable |

Decision needed before apply: <ask-on-risk rules apply>
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low
