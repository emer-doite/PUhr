# Tasks: Hidden Entry Mechanism

## Task List

### T1: Create GestureStep sealed interface

- **Effort**: S (~15 lines)
- **Dependencies**: None
- **Files**: `app/src/main/java/PUhr/clock/gesture/GestureStep.kt` (create)
- **Description**: Sealed interface with five terminal states: `Idle`, `ThreeTapped`, `Swiped`, `LongPressed`, `Triggered`. Plain Kotlin — no Android framework types or imports. Each state is a data object (no payload). `Triggered` is the sole terminal state.
- **Test plan**: Compilation only — not independently testable beyond type existence. Verify all five states are defined.

### T2: Create GestureConfig data class

- **Effort**: S (~35 lines)
- **Dependencies**: None
- **Files**: `app/src/main/java/PUhr/clock/gesture/GestureConfig.kt` (create)
- **Description**: Data class with all resolved gesture parameters. All values in dp or ms. Defaults:
  - `tapToleranceDp = 30f`
  - `tapSequenceTimeoutMs = 2000L`
  - `tapMaxIntervalMs = 300L`
  - `swipeMinLengthDp = 120f`
  - `swipeTimeoutMs = 1500L`
  - `longPressLowerFraction = 0.3f` (bottom 30% of screen height, replaces design's 6-o'clock ±40dp)
  - `longPressHoldMs = 800L`
  - `totalTimeoutMs = 8000L`
- **Test plan**: JUnit test verifying default values via reflection or direct access. Parametrized test for each parameter constructor override.

### T3: Create VaultHomeScreen stub

- **Effort**: S (~15 lines)
- **Dependencies**: None
- **Files**: `app/src/main/java/PUhr/vault/VaultHomeScreen.kt` (create)
- **Description**: Minimal `@Composable` placeholder rendering "Vault" centered text. Package `PUhr.vault`. No callbacks (terminal route). Dark background matching existing theme.
- **Test plan**: Manual — visual confirmation. Optionally `ComposeTestRule` verify text renders.

### T4: Create SecretGestureDetector state machine

- **Effort**: M (~120 lines)
- **Dependencies**: T1 (GestureStep), T2 (GestureConfig)
- **Files**: `app/src/main/java/PUhr/clock/gesture/SecretGestureDetector.kt` (create)
- **Description**: Plain Kotlin class. State machine driving `idle → tapped3 → swiped → longPressed → triggered`. Holds current step, first-tap timestamp (for total timeout), last-tap timestamp (for tap interval), tap count. Methods: `onTap(position: Offset, containerHeightPx: Float)`, `onSwipe(delta: Float)`, `onLongPressStart(position: Offset, containerHeightPx: Float)`, `onLongPressEnd()`. Silent reset to Idle on any deviation. Checks:
  - Tap 1–3: `abs(x - centerX) <= tapToleranceDp` converted via density
  - Tap interval ≤ `tapMaxIntervalMs`
  - All 3 taps within `tapSequenceTimeoutMs` from first tap
  - Swipe: delta.x < 0 (left) and `abs(delta) >= swipeMinLengthDp` within `swipeTimeoutMs`
  - Long-press: `position.y / containerHeightPx >= (1 - longPressLowerFraction)` and hold ≥ `longPressHoldMs`, all within `totalTimeoutMs` from first tap
  - `onTriggerDetected: () -> Unit` callback
  - NO `android.util.Log`, `semantics`, `haptic`, or any Android framework import.
- **Test plan**: Unit tests with `TestCoroutineScheduler` (pure state logic, no Compose needed):
  - Full approved sequence → `onTriggerDetected` fires
  - Tap outside center tolerance → stays/returns Idle
  - Second tap outside center tolerance → resets to Idle
  - Tap interval > 300ms → resets
  - Wrong swipe direction (right) → resets
  - Swipe too short → resets
  - Long-press outside lower 30% → resets
  - Long-press released before 800ms → resets
  - Total timeout (8s) from first tap → resets
  - `tapSequenceTimeoutMs` expires after 2nd tap → resets
  - No logging — compile-time check (verify no `Log` string or `android.util.Log` import in source)

### T5: Integrate SecretGestureDetector on ClockScreen

- **Effort**: M (~50 lines added, ~10 modified)
- **Dependencies**: T4 (SecretGestureDetector)
- **Files**: `app/src/main/java/PUhr/clock/ClockScreen.kt` (modify)
- **Description**:
  - Add `onTriggerDetected: () -> Unit = {}` parameter
  - `remember { SecretGestureDetector(GestureConfig(), onTriggerDetected) }`
  - Wrap outer `Column` with `Modifier.pointerInput(key)` using `awaitPointerEventScope`
  - Own the full pipeline: tap counting → swipe detection → long-press detection
  - Pass container size from `onSizeChanged` or `BoxWithConstraints` for position checks
  - Use `LocalDensity.current` for dp→px conversions
  - Column gets NO `contentDescription`, `semantics {}`, `testTag`, `hapticFeedback`
  - Clock face is unchanged on all partial progress (no visual feedback)
  - Use `forEachGesture` / `awaitEachGesture` for gesture lifecycle
- **Test plan**:
  - `ComposeTestRule` with `performTouchInput`:
    - Full sequence → `onTriggerDetected` invoked
    - Wrong step → callback never invoked
    - Timeout → callback never invoked
  - Anti-forensics: `onNode(hasTestTag(...))` should NOT find the gesture Column. Verify no semantic actions present.

### T6: Scaffold NavHost in MainActivity

- **Effort**: M (~90 lines added/modified)
- **Dependencies**: T5 (ClockScreen with callback), T3 (VaultHomeScreen)
- **Files**: `app/src/main/java/PUhr/MainActivity.kt` (modify)
- **Description**:
  - Replace `setContent { MaterialTheme { ClockScreen() } }` with `NavHost(startDestination = "clock")`
  - Routes:
    - `"clock"` → `ClockScreen(onTriggerDetected = { navController.navigate("auth") { popUpTo("clock") { inclusive = true } } })` + `exitTransition = { fadeOut(tween(400)) }`
    - `"auth"` → `AuthScreen(onVerified = { navController.navigate("vault") { popUpTo("auth") { inclusive = true } } })`. `enterTransition = { scaleIn(0.95f, tween(400)) + fadeIn(tween(400)) }`, `exitTransition = { fadeOut(tween(400)) }`
    - `"vault"` → `VaultHomeScreen()`
  - Auth and vault routes use lambda composable — NOT visible in NavHost graph preview
  - Wrap NavHost with existing `MaterialTheme` and keep `enableEdgeToEdge()`
- **Test plan**:
  - `ComposeTestRule` w/ test `NavHost`:
    - Start → clock route is displayed
    - Simulate trigger sequence → auth route visible, back press exits (no clock in stack)
    - Auth verified → vault route visible, back press exits
  - Manual: verify LaunchedEffect in ClockScreen survives NavHost lifecycle (clock still ticks after route changes and pops)

---

## Dependency Graph

```
T1 ──┐
      ├── T4 ── T5 ── T6
T2 ──┘          │
                │
T3 ─────────────┘
```

### Parallel execution groups

| Group | Tasks | Deps needed | Estimated lines |
|---|---|---|---|
| 1 | T1, T2, T3 | None | 65 |
| 2 | T4 | T1, T2 | 120 |
| 3 | T5 | T4 | 60 |
| 4 | T6 | T5, T3 | 90 |

## Summary

- **Total tasks**: 6
- **Total estimated new lines**: ~335
- **New files**: 4 (GestureStep.kt, GestureConfig.kt, SecretGestureDetector.kt, VaultHomeScreen.kt)
- **Modified files**: 2 (ClockScreen.kt, MainActivity.kt)
- **PR recommendation**: Single PR (~335 lines, well under 400 threshold). No chained PRs needed.
- **Commit structure** (work units):
  1. `feat(clock): add gesture package with state machine types` — T1 + T2
  2. `feat(clock): implement SecretGestureDetector state machine` — T4
  3. `feat(clock): wire gesture detection on ClockScreen` — T5
  4. `feat(nav): scaffold NavHost with hidden routes and animation` — T3 + T6
- **Review workload forecast**: Low (~335 lines across 6 files). Focus areas: correctness of state machine transitions, anti-forensics compliance (no Log/semantics), NavHost lifecycle interaction with ClockScreen's LaunchedEffect.

## Next Step

**sdd-apply**: Execute T1–T6 in dependency order. Start with parallel group 1 (T1, T2, T3), then T4, T5, T6 sequentially.
