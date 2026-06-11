# Tasks: Clock Face

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~220 additions, ~5 deletions |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Foundation + Core + Integration + Tests | Single PR | All tasks tightly coupled; 400-line budget well within limit |

## Phase 1: Foundation

- [x] 1.1 Create `PUhr/clock/ClockViewModel.kt` — `@HiltViewModel` stub class with `@Inject constructor()` for future mode state
- [x] 1.2 Create `LocalTime.formatHMS()` extension producing `"HH:MM:SS"` in utility/companion within `ClockScreen.kt`

## Phase 2: Core Implementation

- [x] 2.1 Create `PUhr/clock/AnalogClockFace.kt` — `@Composable fun AnalogClockFace(hours: Float, minutes: Float, seconds: Float)` drawing face circle, hour/minute/second hands via `rotate()`, and tick marks
- [x] 2.2 Add `Float.toHandDegrees(totalUnits: Int)` extension for computing hand angle from unit count
- [x] 2.3 Create `PUhr/clock/ClockScreen.kt` — `@Composable fun ClockScreen(viewModel: ClockViewModel = hiltViewModel())` with `Column` layout, `LaunchedEffect(withFrameMillis)` for analog sweep, `LaunchedEffect(delay(1000))` for digital readout, `WindowInsetsPadding.systemBars`

## Phase 3: Integration

- [x] 3.1 Modify `MainActivity.kt` — add `enableEdgeToEdge()` in `onCreate`, replace empty `Surface` content with `ClockScreen()`
- [x] 3.2 Modify `AndroidManifest.xml` — add `android:screenOrientation="portrait"` to `MainActivity` `<activity>` tag

## Phase 4: Testing & Verification

- [x] 4.1 Write parameterized unit tests for `toHandDegrees()` — verify REQ-CLOCK-01 scenarios: 3:00:00 → hour=90°, min=0°, sec=0°; 6:00:00 → hour=180°; 0:15:30 → min=90°
- [x] 4.2 Write unit tests for `formatHMS()` — verify REQ-CLOCK-02: 14:30:45 → "14:30:45", 0:0:0 → "00:00:00", 23:59:59 → "00:00:00" rollover
- [x] 4.3 Manual verification on emulator: sweep smoothness, cutout avoidance (REQ-CLOCK-03), portrait lock (REQ-CLOCK-03), no vault UI (REQ-CLOCK-05), animation stops in background (REQ-CLOCK-04)
