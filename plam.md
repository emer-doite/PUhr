# PhantomVault — Android Hidden Storage App

### Full Design & Development Blueprint

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Tech Stack](#2-tech-stack)
3. [Architecture](#3-architecture)
4. [Project File Structure](#4-project-file-structure)
5. [Disguise Layer — The Clock App](#5-disguise-layer--the-clock-app)
6. [Hidden Entry Mechanism](#6-hidden-entry-mechanism)
7. [Authentication Layer](#7-authentication-layer)
8. [Vault Module — Encrypted Storage](#8-vault-module--encrypted-storage)
9. [Security Architecture](#9-security-architecture)
10. [Database Design](#10-database-design)
11. [UI/UX Design System](#11-uiux-design-system)
12. [Anti-Forensics & Stealth Features](#12-anti-forensics--stealth-features)
13. [Performance Optimizations](#13-performance-optimizations)
14. [Testing Strategy](#14-testing-strategy)
15. [Build & Release](#15-build--release)
16. [Roadmap & Future Improvements](#16-roadmap--future-improvements)

---

## 1. Project Overview

**App Name (public):** `Timely` — A minimal clock & alarm app  
**Internal codename:** `PhantomVault`  
**Target:** Android 9.0 (API 28) and above  
**Language:** Kotlin  
**UI framework:** Jetpack Compose  

### Dual Identity

| Surface | What users see | What it actually is |
|---|---|---|
| Launcher icon | A clean clock face | Entry point to the vault |
| Main screen | Analog + binary clock + alarms | The disguise layer |
| Hidden trigger | Invisible interaction | Gateway to vault |

| Vault screen | Full encrypted file manager | The real app |

### Core Principles

- **Plausible deniability** — the app must look and behave like a real clock app at all times
- **Zero-knowledge storage** — even with physical access to the device, files cannot be read without the correct credentials
- **Graceful panic** — a duress PIN/pattern instantly locks or wipes the vault
- **No traces** — the vault does not appear in recents, screenshots are blocked, and the media scanner is excluded

---

## 2. Tech Stack

### Core

| Concern | Library / Tool |
|---|---|
| Language | Kotlin 2.x |

| UI | Jetpack Compose + Material 3 |
| Architecture | Clean Architecture + MVVM + MVI |
| DI | Hilt (Dagger) |
| Async | Kotlin Coroutines + Flow |
| Navigation | Jetpack Navigation Compose |

### Security & Cryptography

| Concern | Library / Tool |
|---|---|
| Symmetric encryption | AES-256-GCM (javax.crypto) |
| Key derivation | Argon2id via `com.lambdapioneer.argon2kt` |
| Secure key storage | Android Keystore System |
| Encrypted DB | Room + SQLCipher (`net.zetetic:android-database-sqlcipher`) |
| Biometrics | AndroidX Biometric (`androidx.biometric`) |
| Secure random | `java.security.SecureRandom` |

### Storage & Files

| Concern | Library / Tool |

|---|---|
| Database | Room 2.x |

| File I/O | Java NIO + Kotlin Extensions |

| Image thumbnails | Coil 3 (with encrypted source decoder) |
| Video thumbnails | MediaMetadataRetriever (scoped) |
| PDF preview | PdfRenderer (Android built-in) |

### Clock & Alarm

| Concern | Library / Tool |
|---|---|
| Alarm scheduling | AlarmManager + WorkManager |
| Notifications | NotificationManager + NotificationCompat |
| Background service | Foreground Service (minimal) |

### Development

| Concern | Library / Tool |

|---|---|
| Build | Gradle + KSP (Kotlin Symbol Processing) |
| Obfuscation | ProGuard + R8 (full mode) |

| Leak detection | LeakCanary (debug only) |
| Unit tests | JUnit5 + MockK |
| UI tests | Compose Testing + Espresso |

---

## 3. Architecture

### Layer Overview

```
┌─────────────────────────────────────────────────────┐
│                  Presentation Layer                  │

│   ClockViewModel  |  VaultViewModel  |  AuthVM       │
│   Compose UI  |  Navigation  |  State Hoisting       │
├─────────────────────────────────────────────────────┤
│                   Domain Layer                       │
│   UseCases: ImportFile, ExportFile, AuthUser,        │

│   SearchFiles, TriggerPanic, ScheduleAlarm, etc.     │
│   Domain Models  |  Repository Interfaces            │

├─────────────────────────────────────────────────────┤
│                    Data Layer                        │
│   Repositories  |  EncryptedFileStore                │
│   Room + SQLCipher  |  Android Keystore              │
│   AlarmRepository  |  SessionManager                 │
└─────────────────────────────────────────────────────┘
```

### State Management (MVI for Vault)

```
UserIntent  →  ViewModel (process)  →  UiState (sealed class)  →  Compose UI
                     ↕

               SideEffects (one-shot events: navigation, toast, biometric prompt)
```

### Navigation Graph

```
NavGraph
├── ClockScreen (start)          ← Public face

│   ├── AlarmListScreen
│   └── AlarmEditScreen
│
└── [HIDDEN] VaultEntryScreen    ← Unlocked only via trigger
    ├── AuthScreen               ← PIN / Biometric
    │   └── VaultHomeScreen
    │       ├── PhotoGridScreen
    │       ├── VideoListScreen

    │       ├── DocumentListScreen

    │       ├── AudioListScreen
    │       ├── AllFilesScreen
    │       ├── FileViewerScreen
    │       ├── ImportScreen

    │       └── SettingsVaultScreen
    └── DuressScreen             ← Shows empty/fake vault
```

---

## 4. Project File Structure

```
phantomvault/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/

│       │   ├── AndroidManifest.xml

│       │   ├── java/com/phantomvault/
│       │   │   │

│       │   │   ├── di/                          # Hilt modules
│       │   │   │   ├── CryptoModule.kt
│       │   │   │   ├── DatabaseModule.kt
│       │   │   │   ├── RepositoryModule.kt
│       │   │   │   └── UseCaseModule.kt
│       │   │   │
│       │   │   ├── core/
│       │   │   │   ├── crypto/
│       │   │   │   │   ├── AesGcmCipher.kt      # AES-256-GCM encrypt/decrypt
│       │   │   │   │   ├── ArgonKeyDeriver.kt   # Argon2id key derivation
│       │   │   │   │   ├── KeystoreManager.kt   # Android Keystore ops
│       │   │   │   │   ├── SecureWipe.kt        # File overwrite before delete
│       │   │   │   │   └── SaltGenerator.kt     # SecureRandom salt factory

│       │   │   │   ├── session/
│       │   │   │   │   ├── SessionManager.kt    # In-memory session key cache
│       │   │   │   │   └── AutoLockTimer.kt     # Clears session after timeout

│       │   │   │   └── util/
│       │   │   │       ├── Extensions.kt
│       │   │   │       └── FlowUtil.kt
│       │   │   │
│       │   │   ├── data/
│       │   │   │   ├── db/
│       │   │   │   │   ├── VaultDatabase.kt     # Room + SQLCipher

│       │   │   │   │   ├── dao/
│       │   │   │   │   │   ├── VaultFileDao.kt
│       │   │   │   │   │   └── AlarmDao.kt
│       │   │   │   │   └── entity/

│       │   │   │   │       ├── VaultFileEntity.kt

│       │   │   │   │       └── AlarmEntity.kt
│       │   │   │   ├── storage/
│       │   │   │   │   ├── EncryptedFileStore.kt # Reads/writes encrypted blobs

│       │   │   │   │   └── ThumbnailCache.kt    # In-memory encrypted thumbnails

│       │   │   │   └── repository/
│       │   │   │       ├── VaultRepositoryImpl.kt
│       │   │   │       └── AlarmRepositoryImpl.kt
│       │   │   │
│       │   │   ├── domain/
│       │   │   │   ├── model/
│       │   │   │   │   ├── VaultFile.kt         # Domain model
│       │   │   │   │   ├── FileCategory.kt      # Enum: IMAGE, VIDEO, AUDIO, DOCUMENT, OTHER
│       │   │   │   │   └── Alarm.kt
│       │   │   │   ├── repository/
│       │   │   │   │   ├── VaultRepository.kt   # Interface
│       │   │   │   │   └── AlarmRepository.kt

│       │   │   │   └── usecase/
│       │   │   │       ├── vault/
│       │   │   │       │   ├── ImportFileUseCase.kt
│       │   │   │       │   ├── ExportFileUseCase.kt

│       │   │   │       │   ├── DeleteFileUseCase.kt
│       │   │   │       │   ├── GetFilesUseCase.kt
│       │   │   │       │   ├── SearchFilesUseCase.kt

│       │   │   │       │   ├── MoveFileUseCase.kt
│       │   │   │       │   └── PanicWipeUseCase.kt
│       │   │   │       └── auth/

│       │   │   │           ├── UnlockVaultUseCase.kt
│       │   │   │           ├── ChangePinUseCase.kt
│       │   │   │           └── ValidateDuressUseCase.kt
│       │   │   │
│       │   │   └── presentation/
│       │   │       ├── clock/

│       │   │       │   ├── ClockScreen.kt       # Main disguise screen
│       │   │       │   ├── ClockViewModel.kt
│       │   │       │   ├── components/
│       │   │       │   │   ├── AnalogClockFace.kt
│       │   │       │   │   ├── BinaryClockDisplay.kt
│       │   │       │   │   └── AlarmCard.kt

│       │   │       │   └── gesture/

│       │   │       │       └── SecretGestureDetector.kt  # Trigger logic
│       │   │       ├── auth/
│       │   │       │   ├── AuthScreen.kt

│       │   │       │   ├── AuthViewModel.kt
│       │   │       │   └── components/
│       │   │       │       ├── PinPad.kt
│       │   │       │       └── BiometricPromptLauncher.kt
│       │   │       └── vault/
│       │   │           ├── VaultHomeScreen.kt
│       │   │           ├── VaultViewModel.kt
│       │   │           ├── FileViewerScreen.kt
│       │   │           ├── ImportScreen.kt
│       │   │           ├── SettingsVaultScreen.kt
│       │   │           └── components/
│       │   │               ├── FileGrid.kt
│       │   │               ├── FileCard.kt
│       │   │               ├── CategoryFilter.kt
│       │   │               ├── SearchBar.kt
│       │   │               └── ImportProgressBar.kt
│       │   │
│       │   └── res/
│       │       ├── drawable/         # Clock assets (SVG/XML)
│       │       ├── font/             # Custom typefaces

│       │       └── values/
│       │           ├── themes.xml
│       │           └── strings.xml   # All strings are neutral/clock-related
│       │
│       ├── test/                     # Unit tests
│       └── androidTest/              # Instrumented tests
│

├── gradle/
│   └── libs.versions.toml            # Version catalog
└── build.gradle.kts
```

---

## 5. Disguise Layer — The Clock App

The clock screen must be convincing and fully functional. This is not a stub.

### 5.1 Clock Display Modes

The clock face has three visual modes toggled by a subtle long-press:

**Mode A — Analog (default)**

- Smooth-sweep second hand (no tick)
- Minimal numerals, strong hour & minute hands
- Dark frosted glass aesthetic, single accent color

**Mode B — Digital + Binary (signature feature)**

- Large digital readout at top (HH:MM:SS)
- Below it: binary representation of the time

  - Six rows of LED-dot indicators: H H : M M : S S
  - Each column is a vertical 4-bit binary number (BCD encoding)
  - Lit dots = 1, unlit dots = 0
- Example: 09:47:32 in BCD binary:

```
  H₁  H₂   M₁  M₂   S₁  S₂
  0    1    0    1    0    1
  0    0    1    0    0    1
  0    0    0    1    1    0
  0    1    0    0    0    0
```

**Mode C — Minimal Zen**

- Only hours and minutes, no seconds
- Extremely large typography, centered
- Dim ambient mode for night

### 5.2 Alarm Functionality

The alarm must be a real, working alarm:

- Create / edit / delete alarms
- Repeat days (Mon–Sun toggles)
- Alarm sound selection (system ringtones)
- Snooze (configurable: 5/10/15 min)
- Scheduled via `AlarmManager.setAlarmClock()` for battery accuracy
- Fallback `WorkManager` for Doze mode reliability
- Alarm fires even when vault is open

### 5.3 Clock Aesthetics

```
Background: Deep charcoal (#0D0D0F) or near-black
Clock face: Subtle radial gradient, no harsh borders
Hour/minute hands: Ivory white (#F5F0E8) — sharp, minimal
Second hand: Thin accent line (user-selectable color)
Binary dots: Small rounded rectangles, lit = accent color, unlit = dim gray
Typography: "Space Grotesk" (display) + "JetBrains Mono" (binary / digital)
Corner decoration: None. No clutter.
Subtle particle ambient: Optional floating dust/stars layer (can be disabled)

```

---

## 6. Hidden Entry Mechanism

The secret trigger must be:

- **Invisible** to a casual observer

- **Memorable** for the user
- **Impossible to stumble upon** accidentally

### 6.1 Recommended: Compound Gesture Sequence

The trigger is a multi-step gesture on the clock face. All steps must be completed within 8 seconds of the first step.

**Default trigger sequence:**

```

Step 1 — Triple-tap the center of the clock face
          (center ±30dp tolerance)

Step 2 — Immediately swipe LEFT across the clock face
          (min 120dp distance, within 1.5s of triple-tap)

Step 3 — Long-press the 6 o'clock position
          (hold 800ms, ±40dp tolerance)
```

If any step fails or times out, the sequence resets silently. No visual feedback is ever shown.

### 6.2 Implementation — `SecretGestureDetector.kt`

```kotlin
class SecretGestureDetector(

    private val onTriggerDetected: () -> Unit
) {
    private val steps = listOf(
        GestureStep.TripleTap(center = true, tolerance = 30.dp),
        GestureStep.Swipe(direction = SwipeDirection.LEFT, minDistance = 120.dp),

        GestureStep.LongPress(clockPosition = ClockPosition.SIX, holdMs = 800)

    )
    private var currentStep = 0
    private var stepTimer: Job? = null

    fun onGestureEvent(event: GestureEvent) {
        if (steps[currentStep].matches(event)) {
            currentStep++
            if (currentStep == steps.size) {

                currentStep = 0
                onTriggerDetected()  // Navigate to vault entry
            } else {
                resetTimerFor(currentStep)
            }
        } else {
            // Wrong gesture: silent reset. No feedback.
            currentStep = 0
            stepTimer?.cancel()
        }
    }

    private fun resetTimerFor(step: Int) {
        stepTimer?.cancel()
        stepTimer = coroutineScope.launch {
            delay(8_000) // 8-second window
            currentStep = 0
        }
    }
}
```

### 6.3 Alternative / Backup Entry

A secondary entry can be configured in vault settings:

| Method | Description |
|---|---|
| **Dial Code** | Open the phone dialer, type a custom code (e.g. `*#7483#`). Registered via `Intent.ACTION_DIAL` + `BroadcastReceiver` |
| **Calculator disguise** | Replace clock with a fake calculator; entering the PIN + `=` unlocks |

| **Volume button sequence** | Hardware button sequence (e.g. Vol↑ Vol↑ Vol↓ Vol↓ long-press Vol↑) |

> **Recommendation:** Enable the dial code method as backup. It works even if the phone is in someone else's hands dialing.

---

## 7. Authentication Layer

### 7.1 Auth Methods (configurable)

```
Primary:    PIN (6–12 digits, no length hint shown on screen)
Secondary:  Biometric (fingerprint / face) — optional, opt-in
Duress:     Separate PIN that opens a fake/empty vault
```

### 7.2 PIN Entry Design

- No keyboard hint of digit count (dots grow as user types, but no empty slots visible)
- No "wrong PIN" error message — wrong PIN just clears silently and shakes subtly

- After **5 consecutive failures**: 30-second cool-down (doubles with each additional failure)
- After **15 total failures**: optional panic wipe (user-configurable)
- PIN is **never stored** — only the Argon2id-derived key hash is stored for verification

### 7.3 Key Derivation Flow

```
User PIN
    │

    ▼
Argon2id KDF ────────── random 128-bit salt (stored encrypted in Keystore)
    │  (t=3 iterations, m=65536 KB, p=4 threads)
    ▼
256-bit Master Key
    │
    ├──► Vault Encryption Key (VEK)  ← used to encrypt/decrypt files
    └──► Database Key (DBK)          ← used to open SQLCipher database
```

### 7.4 Biometric Integration

Biometrics unlock a **Keystore-wrapped** copy of the Master Key — they never replace the PIN.

```kotlin
// Store key for biometric
val keyEntry = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
val encryptedMasterKey = cipher.doFinal(masterKey)  // Keystore-backed cipher

// Biometric only decrypts the wrapper — the actual key never leaves Keystore
```

**Why this matters:** If someone forces biometric (e.g. thumb pressed while asleep), they still cannot export the key. The session is time-limited.

### 7.5 Duress PIN

When the duress PIN is entered:

- The vault UI opens normally (no panic look)
- All real files are hidden from the file list

- An optional set of "fake" decoy files can be shown
- A silent notification/log is recorded with timestamp + location (optional)
- The duress session has read-only access — no file can be imported or exported

---

## 8. Vault Module — Encrypted Storage

### 8.1 Encrypted File Store Architecture

Files are never stored as-is. Every file goes through:

```
Original File
    │
    ▼
Generate random 96-bit IV (per file)
    │
    ▼
AES-256-GCM encrypt(file_bytes, VEK, IV)
    │
    ▼
Write: [4-byte header magic] + [IV (12 bytes)] + [ciphertext] + [auth tag (16 bytes)]
    │
    ▼
Save to: /data/data/com.phantomvault/files/vault/{random_uuid}.enc
```

The original filename, type, and metadata are **only** stored in the encrypted SQLCipher database, never in the filename or on disk.

### 8.2 Storage Location

```
Internal storage only: Context.filesDir + "/vault/"
```

- `filesDir` is sandboxed — other apps cannot access it
- Not exposed to `MediaStore` — files are invisible to gallery apps
- Not backed up via ADB (`android:allowBackup="false"` in manifest)
- Not accessible via USB MTP by default
- Add `.nomedia` file to prevent any media scanner indexing

### 8.3 File Import Process

```
1. User selects file via DocumentsProvider (system file picker)
2. App reads file via ContentResolver (temp in memory or temp file)

3. Generate random UUID for internal filename
4. Encrypt with AES-256-GCM
5. Write encrypted blob to vault directory

6. Store metadata in encrypted Room/SQLCipher DB
7. Generate thumbnail (image/video/PDF) → encrypt thumbnail separately
8. Securely zero-fill and delete the temp file if any
9. Optionally: delete the original from its source location (user prompt)
```

### 8.4 File Categories

```kotlin
enum class FileCategory(val mimeTypes: List<String>) {
    IMAGE(listOf("image/*")),
    VIDEO(listOf("video/*")),
    AUDIO(listOf("audio/*")),
    DOCUMENT(listOf("application/pdf", "application/msword",
                     "application/vnd.openxmlformats*", "text/plain")),
    ARCHIVE(listOf("application/zip", "application/x-rar-compressed")),
    OTHER(listOf("*/*"))

}
```

### 8.5 Vault Home Screen Layout

```
┌─────────────────────────────────────────┐
│  [🔍 Search]              [⋮ Menu]      │

│                                          │
│  📷 Photos     🎬 Videos     🎵 Audio   │

│   142 files     38 files      67 files   │
│                                          │
│  📄 Documents  📦 Archives   📁 All     │
│   29 files      5 files      281 files   │
│                                          │
│  ─────────────────────────────────────  │
│  Recent Files                            │
│  [thumb] vacation.jpg        2h ago      │
│  [thumb] document.pdf        1d ago      │
│  [thumb] backup.zip          3d ago      │
│                                          │
│  Storage: ██████░░░░  2.4 GB / 8.0 GB   │
└─────────────────────────────────────────┘
```

### 8.6 File Viewer

- **Images:** Zoomable viewer, rendered fully in-memory (never written to a temp file)
- **Videos:** MediaPlayer with custom Surface — no external intent, no thumbnails exposed
- **PDFs:** Android `PdfRenderer` — pages decrypted one at a time, rendered to a Bitmap, then discarded
- **Audio:** MediaPlayer in foreground service — track name never appears in system media controls (use a neutral title)
- **Documents:** WebView sandboxed renderer for plain text / HTML; no external apps

---

## 9. Security Architecture

### 9.1 Threat Model

| Threat | Mitigation |
|---|---|
| Physical device theft (locked) | Android FDE + app's own encryption |
| Physical device theft (unlocked) | App auto-locks after timeout (configurable: 30s–5min) |
| Someone sees the screen briefly | App looks like a clock |
| Someone knows about the app | Still needs PIN/biometric to enter |

| Forced PIN extraction | Duress PIN shows empty/fake vault |
| Brute-force PIN attempt | Progressive lockout + optional wipe |
| Malware / other apps reading vault | Internal `filesDir` — not accessible to other apps |
| Backup extraction (ADB) | `android:allowBackup="false"` |
| Cloud backup extraction | All sensitive data excluded from BackupAgent |
| Memory dump (rooted device) | Session key zeroized on lock / background |
| Forensic file recovery | Secure wipe (3-pass overwrite) before file deletion |
| Screenshot of vault content | `FLAG_SECURE` on vault window |
| App switcher leak | Vault screens blurred in app switcher thumbnail |

| Network exfiltration | App requests zero network permissions |

### 9.2 Encryption Specification

```
Algorithm:      AES-256-GCM
Key size:       256 bits
IV size:        96 bits (random per file, per operation)
Auth tag:       128 bits
KDF:            Argon2id
  - Memory:     65,536 KB (64 MB)

  - Iterations: 3
  - Parallelism: 4
  - Salt:       128 bits (random per installation)
Key wrapping:   Android Keystore (hardware-backed when available)

DB encryption:  SQLCipher with 256-bit key
```

### 9.3 Android Keystore Integration

```kotlin
object KeystoreManager {
    private const val ALIAS = "pv_master_key_wrap"

    fun generateWrappingKey() {
        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")

        keyGen.init(
            KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false) // PIN is our own auth
                .setInvalidatedByBiometricEnrollment(true)
                .setUnlockedDeviceRequired(true)  // Requires device to be unlocked
                .build()

        )
        keyGen.generateKey()

    }

}
```

**Hardware-backed keys:** On devices with a Trusted Execution Environment (TEE) or StrongBox, keys never leave the secure hardware — even a rooted OS cannot extract them.

### 9.4 Session Management

```kotlin
class SessionManager @Inject constructor() {
    // Key is held in a ByteArray in memory only — never persisted during session
    private var vaultEncryptionKey: ByteArray? = null
    private val lockJob: Job? = null


    fun openSession(key: ByteArray, autoLockSeconds: Int) {
        vaultEncryptionKey = key.copyOf()
        scheduleLock(autoLockSeconds)
    }

    fun closeSession() {
        vaultEncryptionKey?.fill(0)  // Zero-fill before GC
        vaultEncryptionKey = null
        lockJob?.cancel()
    }

    // Called from: app backgrounded, timer expired, duress, explicit lock
}
```

### 9.5 Panic Wipe

Two levels configurable by the user:

| Level | Action |
|---|---|
| **Soft wipe** | Delete all encrypted files + wipe DB. Keys gone. Files unrecoverable. |

| **Hard wipe** | Soft wipe + overwrite vault directory 3× with random bytes + delete app data |

Triggers:

- Duress PIN (optional — user configures)
- PIN failure threshold (optional)
- Manual "Destroy Vault" in settings (requires PIN confirmation)
- Remote trigger via SMS/notification (optional advanced feature)

```kotlin

class PanicWipeUseCase @Inject constructor(
    private val fileStore: EncryptedFileStore,
    private val database: VaultDatabase,
    private val keystoreManager: KeystoreManager
) {
    suspend fun execute(level: WipeLevel) {
        // 1. Delete all .enc files with secure wipe
        fileStore.secureWipeAll()
        // 2. Drop and recreate the encrypted DB
        database.close()
        database.deleteDatabase()
        // 3. Delete Keystore entry (key permanently gone)
        keystoreManager.deleteKey()
        // 4. Hard level: overwrite directory 3× with random bytes
        if (level == WipeLevel.HARD) {
            fileStore.overwriteDirectory(passes = 3)
        }
    }
}
```

---

## 10. Database Design

### SQLCipher + Room Schema

#### `VaultFileEntity`

```kotlin
@Entity(tableName = "vault_files")
data class VaultFileEntity(
    @PrimaryKey val id: String,          // UUID
    val encryptedName: ByteArray,        // Original filename, AES-encrypted

    val fileCategory: String,            // IMAGE / VIDEO / AUDIO / DOCUMENT / OTHER
    val mimeType: String,
    val sizeBytes: Long,
    val dateAdded: Long,                 // Unix timestamp
    val dateModified: Long,
    val hasThumbnail: Boolean,
    val thumbnailId: String?,            // UUID of encrypted thumbnail blob
    val folderPath: String,              // Virtual folder path (encrypted)

    val notes: ByteArray?,               // User note, encrypted
    val iv: ByteArray,                   // IV used for filename/notes encryption
    val checksum: ByteArray             // BLAKE2b hash for integrity check
)
```

#### `AlarmEntity`

```kotlin
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey val id: Int,
    val label: String,
    val hour: Int,
    val minute: Int,
    val repeatDays: Int,       // Bitmask: Mon=1, Tue=2, Wed=4 ... Sun=64
    val isEnabled: Boolean,
    val ringtoneUri: String,

    val snoozeDurationMin: Int,
    val vibrate: Boolean
)

```

### Integrity Check

On every vault open, a BLAKE2b checksum of critical DB entries is verified. If tampering is detected, the vault refuses to open and the user is alerted.

---

## 11. UI/UX Design System

### 11.1 Design Tokens

```
Background:      #0A0A0C  (near-black, warm undertone)
Surface:         #141416
Surface Variant: #1E1E22
Primary:         #C8A96E  (muted gold — clock hand accent)
Secondary:       #6E8DC8  (soft blue — binary clock lit dots)
Error:           #CF6679
Text Primary:    #F2EDE4
Text Secondary:  #8A8A94
Binary Lit:      #6E8DC8  with 0.9 opacity
Binary Unlit:    #2A2A32  with 0.6 opacity


Typography:

  Display:   "Space Grotesk" — used for time digits
  Mono:      "JetBrains Mono" — used for binary display
  Body:      "Inter" — used for labels and menus
  Scale:     12sp / 14sp / 16sp / 20sp / 32sp / 64sp

Corners:
  Cards: 16dp
  Buttons: 12dp
  Chips: 8dp

Motion:
  Standard: 250ms EaseInOut
  Enter:    300ms EaseOut
  Exit:     200ms EaseIn
  No spring animations in vault (looks too playful)
```

### 11.2 Clock Screen Visual Hierarchy

```
┌─────────────────────────────────────────┐
│                                          │
│                                          │

│         ┌──────────────────┐            │
│         │   [Analog clock  │            │
│         │   face — center] │            │
│         │                  │            │
│         └──────────────────┘            │
│                                          │

│    09 : 47 : 32   ← digital             │

│    ●○○○  ●○○●  ○○●●  ○●○○   ← binary   │
│    H           M           S             │
│                                          │
│  ─────────────────────────────────────  │
│  ☾ Wed, Jun 10         + New Alarm      │
│  07:00  Wake up            ●            │
│  22:30  Night alarm        ○            │
└─────────────────────────────────────────┘
```

### 11.3 Vault UI — Visual Language

The vault uses a slightly different visual tone to signal "you are in the secure zone":

- Background shifts to a slightly deeper tone (#08080A)
- A barely visible `🔒` shimmer at top of screen for 2 seconds on entry
- File cards use frosted glass treatment
- No app name visible anywhere in the vault UI

---

## 12. Anti-Forensics & Stealth Features

### 12.1 App Manifest Hardening

```xml
<application
    android:allowBackup="false"
    android:fullBackupContent="false"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:networkSecurityConfig="@xml/network_security_config"

    android:debuggable="false"
    android:usesCleartextTraffic="false"
    android:label="Timely"

    android:icon="@mipmap/ic_launcher_clock">

    <!-- Vault activities: exclude from recents -->
    <activity
        android:name=".presentation.vault.VaultActivity"
        android:excludeFromRecents="true"
        android:noHistory="false"
        android:screenOrientation="portrait" />
```

### 12.2 Screenshot & Screen Recording Prevention

```kotlin
// Applied to all vault screens
WindowManager.LayoutParams.FLAG_SECURE

// In Compose:
DisposableEffect(Unit) {
    val window = (context as Activity).window
    window.setFlags(FLAG_SECURE, FLAG_SECURE)

    onDispose {
        window.clearFlags(FLAG_SECURE)
    }
}
```

### 12.3 App Switcher (Recents) Blur

```kotlin
override fun onPause() {
    super.onPause()
    if (isVaultOpen) {

        window.addFlags(FLAG_SECURE)
        // Replace task thumbnail with a clock screenshot
        setTaskDescription(ActivityManager.TaskDescription(
            "Timely", clockBitmap, Color.BLACK
        ))
    }
}

```

### 12.4 Requested Permissions (Minimal)

```xml
<!-- Clock app permissions only -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
<uses-permission android:name="android.permission.USE_FINGERPRINT" />

<!-- File import — ONLY while importing, via DocumentsProvider. No broad storage permission. -->
<!-- Explicitly DO NOT request READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE -->
<!-- Explicitly DO NOT request INTERNET or ACCESS_NETWORK_STATE -->
<!-- Explicitly DO NOT request CAMERA -->
```

> Requesting zero network permissions means even a malicious update cannot phone home.

### 12.5 Anti-Debugging (Release Build)

```kotlin

// In Application.onCreate for release builds
if (BuildConfig.DEBUG.not()) {
    if (Debug.isDebuggerConnected()) {
        // Close the vault session immediately
        sessionManager.closeSession()
    }
}
```

### 12.6 Secure Delete

```kotlin
object SecureWipe {
    fun wipeFile(file: File, passes: Int = 3) {
        val size = file.length()
        val raf = RandomAccessFile(file, "rws")
        repeat(passes) {

            raf.seek(0)
            val randomBytes = ByteArray(4096)
            var written = 0L
            while (written < size) {
                SecureRandom().nextBytes(randomBytes)
                val toWrite = minOf(randomBytes.size.toLong(), size - written).toInt()
                raf.write(randomBytes, 0, toWrite)
                written += toWrite
            }
            raf.fd.sync()
        }
        raf.close()
        file.delete()
    }
}
```

### 12.7 Memory Security

- Sensitive `ByteArray` values (PIN hash, derived keys) are zero-filled (`fill(0)`) immediately after use
- No PIN or key material ever stored in a Kotlin `String` (strings are immutable and cannot be zeroed)
- Thumbnails decrypted only when the grid is visible; evicted from memory when off-screen
- `ViewModel.onCleared()` always closes the session

---

## 13. Performance Optimizations

### 13.1 Encryption Performance

- Chunked streaming encryption for large files (4 MB chunks) to prevent OOM

- Encryption/decryption runs on `Dispatchers.IO` — never blocks the main thread
- Hardware AES acceleration is used automatically on ARMv8 devices (AES-NI equivalent)

### 13.2 Thumbnail Strategy

```
On import:
  - Generate thumbnail (256×256 for images, 128×128 for video)
  - Encrypt thumbnail with separate IV (stored in DB)
  - Cache in memory LRU (Coil MemoryCache, 64 MB max)

On display:
  - Coil loads from encrypted blob → decrypts in background → renders
  - Never writes decrypted thumbnail to disk
  - Evicted immediately when LazyGrid item scrolls out of range

```

### 13.3 Clock Performance

- Clock ticks driven by `Flow<LocalTime>` using `conflate()` — never accumulates backpressure
- Binary clock recalculates only when seconds change (not every composition)
- Analog clock uses `Canvas` DrawScope — no allocations per frame after initial setup
- Battery mode: when display dims, switch to 1-minute updates only

### 13.4 Large Vault Handling

- `VaultFile` list uses Paging 3 (30 items per page) for vaults with 1000+ files
- SQLite query indices on `dateAdded`, `fileCategory`, and `folderPath`
- File metadata search uses FTS4 (Full-Text Search) on encrypted-then-indexed names

---

## 14. Testing Strategy

### 14.1 Unit Tests

```
core/crypto/
  ✓ AesGcmCipher: encrypt-decrypt round-trip
  ✓ AesGcmCipher: tampered ciphertext throws AEADBadTagException
  ✓ ArgonKeyDeriver: same PIN + salt always produces same key
  ✓ ArgonKeyDeriver: different salts produce different keys
  ✓ SecureWipe: file bytes are randomized after wipe

domain/usecase/
  ✓ ImportFileUseCase: file imported → exists in repository
  ✓ DeleteFileUseCase: file deleted → securely wiped from disk
  ✓ PanicWipeUseCase: all files removed from disk and DB
  ✓ UnlockVaultUseCase: correct PIN → session opened

  ✓ UnlockVaultUseCase: wrong PIN → session not opened
  ✓ ValidateDuressUseCase: duress PIN → duress session opened

presentation/
  ✓ SecretGestureDetector: correct sequence → trigger
  ✓ SecretGestureDetector: wrong sequence → no trigger
  ✓ SecretGestureDetector: timeout resets step counter
```

### 14.2 Integration Tests

```
  ✓ Full import → view → export cycle (in-memory DB)
  ✓ Vault opens with correct PIN (real Keystore on emulator)
  ✓ Vault rejects incorrect PIN
  ✓ Duress PIN shows empty vault
  ✓ Panic wipe leaves no recoverable files
  ✓ Auto-lock fires after configured timeout
  ✓ App backgrounded → vault locks → re-entry requires PIN
```

### 14.3 Security Tests

```
  ✓ Verify FLAG_SECURE prevents screenshot in instrumented test

  ✓ Verify vault directory returns no files to other apps via FileProvider

  ✓ Verify no vault content appears in Android MediaStore

  ✓ Verify DB file is not readable without SQLCipher key
  ✓ Verify .enc files have no detectable headers after encryption
```

---

## 15. Build & Release

### 15.1 Build Variants

```

debug:
  - Logging enabled (Timber)
  - LeakCanary included
  - ProGuard disabled
  - FLAG_SECURE disabled (for UI testing)

release:
  - All logging stripped
  - R8 full mode enabled
  - ProGuard aggressive obfuscation
  - FLAG_SECURE enforced
  - android:debuggable="false" enforced
```

### 15.2 ProGuard Rules

```proguard
# Keep Room entities
-keep class com.phantomvault.data.db.entity.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }


# Keep Argon2 JNI
-keep class com.lambdapioneer.argon2kt.** { *; }

# Keep SQLCipher
-keep class net.sqlcipher.** { *; }

# Obfuscate everything else aggressively
-obfuscationdictionary obfuscation-dict.txt
-classobfuscationdictionary obfuscation-dict.txt

-packageobfuscationdictionary obfuscation-dict.txt
```

### 15.3 Distribution

- **Not recommended for Google Play Store** (Play Protect scans may flag encrypted file storage; also requires disclosure)
- Distribute as a **signed APK** via direct download or F-Droid (open source variant)
- Sign with a dedicated keystore, stored offline in encrypted backup
- Use **APK signature scheme v3** for maximum tamper resistance

### 15.4 Signing

```bash
# Generate keystore (do once, store safely)
keytool -genkeypair -v \

  -keystore phantomvault-release.jks \
  -keyalg RSA -keysize 4096 \
  -validity 10000 \
  -alias phantomvault


# Sign APK
./gradlew assembleRelease

# Signed automatically via signingConfigs in build.gradle.kts
```

---

## 16. Roadmap & Future Improvements

### Phase 1 — MVP (Weeks 1–6)

- [x] Working clock + alarm app (fully functional disguise)
- [x] Secret gesture trigger
- [x] PIN authentication + Argon2id KDF
- [x] AES-256-GCM file encryption
- [x] SQLCipher database
- [x] Import / view / delete files
- [x] Photo and video support
- [x] FLAG_SECURE + app switcher protection

### Phase 2 — Hardening (Weeks 7–10)

- [ ] Biometric unlock (with Keystore-backed key)
- [ ] Duress PIN + fake vault
- [ ] Auto-lock timer (configurable)
- [ ] Panic wipe (soft + hard)
- [ ] Secure file deletion (3-pass overwrite)

- [ ] Binary clock polish + theme selector

- [ ] PDF and audio file support

### Phase 3 — Advanced (Weeks 11–16)

- [ ] Virtual folders (organize files like albums)
- [ ] In-vault camera (photos saved directly to vault, never in gallery)
- [ ] In-vault note editor (encrypted text notes)
- [ ] Decoy vault (realistic fake files for duress scenario)
- [ ] Hidden calculator entry method (alternative trigger)
- [ ] Custom alarm sounds (stored in vault, not system)
- [ ] Remote wipe via SMS (fire-and-forget, no internet required)

### Phase 4 — Polish & Extras

- [ ] Widget clock (real widget — strengthens disguise)
- [ ] Animated binary clock (smooth LED transitions)
- [ ] Vault export (encrypted backup blob, password-protected)
- [ ] Multi-language support
- [ ] Accessibility (TalkBack must not announce vault labels)
- [ ] Tablet layout support

---

## Appendix A — Security Checklist Before Release

```
[ ] android:allowBackup="false" verified in manifest

[ ] android:debuggable="false" in release manifest
[ ] No INTERNET permission in manifest
[ ] No READ_EXTERNAL_STORAGE in manifest
[ ] FLAG_SECURE applied to all vault activities/composables
[ ] All log statements removed from release build (Timber.plant only in debug)
[ ] ProGuard/R8 obfuscation verified (check APK with apktool)

[ ] SQLCipher database not readable without key (verify with DB browser)
[ ] .enc files have no identifiable magic bytes or headers
[ ] Vault directory excluded from Android Backup
[ ] .nomedia file present in vault directory
[ ] Hardware key attestation enabled where available
[ ] Argon2 parameters tested on lowest supported device (timing > 300ms is acceptable)
[ ] Session key zero-filled on app background confirmed via memory dump test

[ ] Panic wipe leaves no recoverable file structure (test with recovery tools)
```

---

## Appendix B — Key Dependencies (`libs.versions.toml`)

```toml
[versions]
kotlin = "2.0.0"
compose-bom = "2024.05.00"
hilt = "2.51"
room = "2.6.1"
sqlcipher = "4.5.6"
argon2 = "2.0.0"
coil = "3.0.0"
biometric = "1.2.0-alpha05"
coroutines = "1.8.1"

[libraries]
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
sqlcipher = { module = "net.zetetic:android-database-sqlcipher", version.ref = "sqlcipher" }
argon2 = { module = "com.lambdapioneer.argon2kt:argon2kt", version.ref = "argon2" }

coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
biometric = { module = "androidx.biometric:biometric", version.ref = "biometric" }
```

---

*PhantomVault Blueprint — v1.0 | All security recommendations are conservative by design.*
*The app requests the minimum possible permissions and stores nothing in plaintext.*
