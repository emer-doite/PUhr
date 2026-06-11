# Design: Clock Face — Primary Disguise Layer

## Technical Approach

Replace the empty `Surface` in `MainActivity` with `ClockScreen`, which composes an analog clock (Compose `Canvas` `DrawScope`) and a digital HH:MM:SS readout (`Text` with `FontFamily.Monospace`). Time sourced from `java.time.LocalTime` (API 26+, no new dependency needed). Analog sweeps at 60fps via `LaunchedEffect` + `withFrameMillis`; digital updates every 1s via `LaunchedEffect` + `delay`. `ClockViewModel` holds mode state (stub for future modes). Full-screen via `enableEdgeToEdge()` + `WindowInsetsPadding.systemBars`. Portrait lock via manifest `android:screenOrientation="portrait"`.

## Architecture Decisions

| Decision | Choice | Alternatives | Rationale |
|----------|--------|-------------|-----------|
| Animation driver | `withFrameMillis` in `LaunchedEffect` | `Animatable` / `animate*AsState` | Fine-grained angle interpolation; coroutine cancels when composition leaves composition, stopping background frames naturally |
| Time source | `java.time.LocalTime.now()` | `kotlinx.datetime.Clock.System` | No new dependency needed; minSdk 28 guarantees `java.time` availability |
| Portrait lock | `AndroidManifest.xml` attribute | `requestedOrientation` at runtime | Declarative, compile-time enforced, no permission/runtime code needed |
| ViewModel | `ClockViewModel` via Hilt | No ViewModel, pure state hoisting | Follows existing Hilt pattern; mode state will grow with future changes |
| Package structure | `PUhr/clock/` | Flat `PUhr/` | Isolates disguise layer from vault internals; clean separation per Blueprint §11.2 |

## Data Flow

```
LaunchedEffect(withFrameMillis) ──→ LocalTime.now() ──→ calcAngles() ──→ AnalogClockFace(canvas)
                                                                                │
                                                                                ├─ Draw circle face
                                                                                ├─ Draw hour/min/sec hands via rotate()
                                                                                └─ Draw tick marks

LaunchedEffect(delay(1000)) ──→ LocalTime.now() ──→ format HH:MM:SS ──→ Text(fontFamily = Monospace)

ClockViewModel ←── (future: mode state)
```

Both effects run inside `ClockScreen` composable. Canvas on top, digital readout below, centered vertically in a `Column`.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `PUhr/clock/ClockScreen.kt` | Create | Orchestrator composable — hosts both effects, Canvas + Text layout |
| `PUhr/clock/AnalogClockFace.kt` | Create | Pure `Canvas` composable drawing clock face, hands, tick marks |
| `PUhr/clock/ClockViewModel.kt` | Create | Hilt ViewModel, holds state (stub for future modes) |
| `MainActivity.kt` | Modify | Add `enableEdgeToEdge()`, replace `Surface` content with `ClockScreen` |
| `AndroidManifest.xml` | Modify | Add `android:screenOrientation="portrait"` to `MainActivity` |

## Interfaces / Contracts

```kotlin
// AnalogClockFace.kt — pure drawing composable
@Composable
fun AnalogClockFace(
    hours: Float,     // 0..12 (0 = 12 o'clock)
    minutes: Float,   // 0..60
    seconds: Float    // 0..60 (continuous for sweep)
)

// ClockScreen.kt
@Composable
fun ClockScreen(
    viewModel: ClockViewModel = hiltViewModel()
)

// ClockViewModel.kt
@HiltViewModel
class ClockViewModel @Inject constructor() : ViewModel() {
    // Stub: holds mode enum in future
}
```

Angle calculation (owned by `AnalogClockFace.kt` or pure utility):
```kotlin
fun Float.toHandDegrees(totalUnits: Int): Float =
    this / totalUnits * 360f
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | Angle calculation: `toHandDegrees()` | Pure function — parameterized tests: 3:00:00 → hour=90°, min=0°, sec=0° |
| Unit | Formatting: `LocalTime` → `"HH:MM:SS"` | Pure function: 14:30:45 → `"14:30:45"`, 0:0:0 → `"00:00:00"` |
| Integration | `ClockScreen` renders without crash | Compose `createComposeRule()` + `onNodeWithText` |
| Manual | Canvas sweep, hand proportions, cutout overlap | Device/emulator visual inspection |

## Migration / Rollout

No migration required. This is a greenfield UI change with no data or state migration.

## Open Questions

- [ ] Should angles be computed in `AnalogClockFace` or extracted to a pure utility for testability? (Recommend: pure utility in same file)
- [ ] Does `withFrameMillis` consume noticeable battery on device? Mitigation: it naturally stops when composition is removed (app in background).
