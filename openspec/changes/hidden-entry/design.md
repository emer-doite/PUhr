# Design: Hidden Entry Mechanism

## Technical Approach

Add a compound gesture detector (`SecretGestureDetector`) on `ClockScreen`'s outer `Column` that drives a state machine through `idle → tapped3 → swiped → longPressed → triggered`. On trigger, `MainActivity`'s new `NavHost` navigates to `AuthScreen` with an animated scale-fade transition, popping the clock route from the back stack for anti-forensic clean exit.

## Architecture Decisions

### Decision: Gesture API — Raw `awaitPointerEventScope`

**Choice**: Use `Modifier.pointerInput` with `awaitPointerEventScope` directly (Approach 3 from exploration).
**Alternatives**: `detectTapGestures` + `detectHorizontalDragGestures` composed separately (Approach 1).
**Rationale**: The compound sequence needs to own the event pipeline across tap/swipe/long-press boundaries. Split detectors cannot coordinate — e.g., `detectTapGestures` consumes the tap and returns, leaving no way to hand off to swipe detection. Raw pointer event scope lets one coroutine own the full sequence, cancel on timeout, and reset atomically.

### Decision: State Location — Composable-local `remember`

**Choice**: `SecretGestureDetector` instance created via `remember { SecretGestureDetector(...) }` inside `ClockScreen`.
**Alternatives**: ViewModel-scoped state, singleton.
**Rationale**: Gesture state is pure UI — no business logic, no persistence, no reason to survive config changes. ViewModel would over-scope the state and add test friction for what is fundamentally a UI gesture machine.

### Decision: Navigation — `NavHost` with `popUpTo`

**Choice**: `NavHost(startDestination = "clock")` with routes `clock`, `auth`, `vault`. On trigger, `navController.navigate("auth") { popUpTo("clock") { inclusive = true } }`.
**Alternatives**: Visibility toggle (`if (showAuth)`), single-Activity with nav tags.
**Rationale**: `NavHost` gives clean back-stack management, built-in animation support via `enterTransition`/`exitTransition`, and standard Android navigation patterns. `popUpTo(inclusive=true)` ensures the clock route is removed — pressing back from auth exits the app, leaving no forensic trace in the back stack.

### Decision: Animation — NavHost `enterTransition`

**Choice**: Custom `enterTransition` on `composable("auth")` with `scaleIn(0.95f) + fadeIn()` over 400ms. Clock route exit gets `fadeOut`.
**Alternatives**: `AnimatedNavHost`, manual `AnimatedContent`.
**Rationale**: `NavHost` in Compose Navigation 2.8+ supports `enterTransition`/`exitTransition` natively on `composable()`. No need for `AnimatedNavHost` wrapper. The scale-fade is subtle enough to not draw attention while providing smooth visual feedback.

### Decision: Anti-Forensics — Zero observability contract

**Choice**: `SecretGestureDetector` is a plain Kotlin class in `PUhr.clock.gesture` that never imports `android.util.Log`. No `contentDescription`, `semantics {}`, or `testTag` on the gesture container `Column`. No haptic feedback via `LocalHapticFeedback`. No animation or color change on partial gesture completion.
**Alternatives**: Using Timber or conditional logging.
**Rationale**: The spec (REQ-HIDE-03) explicitly forbids any forensic trace. Plain Kotlin classes cannot log by accident. The Compose modifier chain on the Column intentionally excludes semantics and haptic modifiers.

## Gesture State Machine

```
GestureStep (sealed interface):
├── Idle          — waiting for first tap
├── ThreeTapped   — 3 center-taps within tapSequenceTimeoutMs
├── Swiped        — horizontal left swipe ≥ swipeMinLengthDp
├── LongPressed   — hold at 6-o'clock position ≥ longPressHoldMs
└── Triggered     — final state, fires onTriggerDetected

Transitions (all self-transitions to Idle on timeout/error):
Idle ──(3 taps within tolerance & timeout)──→ ThreeTapped
ThreeTapped ──(left swipe ≥ min, within swipeTimeoutMs)──→ Swiped
Swiped ──(long-press at 6-o'clock ≥ holdMs, within totalTimeout)──→ LongPressed
LongPressed ──(hold released after threshold)──→ Triggered
```

Total wall-clock timeout from first tap: `totalTimeoutMs` (8000ms). Every wrong gesture, wrong position, or timeout resets silently to `Idle`.

## GestureConfig

| Parameter | Default | Source |
|---|---|---|
| `tapToleranceDp` | 30dp | REQ-HIDE-02 |
| `tapSequenceTimeoutMs` | 2000 | REQ-HIDE-02 |
| `tapMaxIntervalMs` | 300 | REQ-HIDE-02 |
| `swipeMinLengthDp` | 120dp | REQ-HIDE-02 |
| `swipeTimeoutMs` | 1500 | REQ-HIDE-02 |
| `longPressToleranceDp` | 40dp | REQ-HIDE-02 |
| `longPressHoldMs` | 800 | REQ-HIDE-02 |
| `totalTimeoutMs` | 8000 | REQ-HIDE-02 |

## Data Flow

```
User touch ──→ ClockScreen.Column (pointerInput)
                   │
                   ▼
          SecretGestureDetector
          (remembers step, timestamps)
                   │
                   ▼ (on triggered)
          onTriggerDetected()
                   │
                   ▼
          navController.navigate("auth")
          { popUpTo("clock") { inclusive = true } }
                   │
                   ▼
          AuthScreen ──→ onVerified ──→ navigate("vault")
                                            │
                                            ▼
                                        VaultHomeScreen
```

## File Changes

| File | Action | Description |
|---|---|---|
| `PUhr/clock/gesture/GestureStep.kt` | Create | Sealed interface for state machine states |
| `PUhr/clock/gesture/GestureConfig.kt` | Create | Data class for gesture parameters |
| `PUhr/clock/gesture/SecretGestureDetector.kt` | Create | Plain Kotlin state machine, no Android deps |
| `PUhr/clock/ClockScreen.kt` | Modify | Add `onTriggerDetected` param, `pointerInput` on Column |
| `PUhr/MainActivity.kt` | Modify | Replace direct `ClockScreen()` with `NavHost` |
| `PUhr/auth/AuthScreen.kt` | Modify | No functional change — already accepts `onVerified` |
| `PUhr/vault/VaultHomeScreen.kt` | Create | Minimal stub for "vault" route |

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | `SecretGestureDetector` step advancement, timeout, reset, trigger | Compose `pointerInput` not needed — test state machine logic with `TestCoroutineScheduler`, advance past timeouts, assert state transitions |
| Unit | `GestureConfig` overrides | Plain JUnit — construct with overrides, assert behavior |
| Integration | ClockScreen gesture pipeline | `ComposeTestRule` with `performTouchInput` simulating full sequence, assert `onTriggerDetected` call |
| Integration | NavHost routing | `ComposeTestRule` + `NavHost` in test, assert route changes on trigger |
| Anti-forensic | No logging | `JUnit` test that loads `SecretGestureDetector` class and verifies no `Log` imports via classloader or bytecode check (alternatively: manual code review enforced by spec) |
| Anti-forensic | No semantics on gesture area | `ComposeTestRule` with `onNode` — assert no semantic modifier on gesture container |

## Migration / Rollout

No migration required. This is a pure additive change — existing clock rendering is untouched. The NavHost refactor changes the composable root but produces identical initial output. Verify that the clock `LaunchedEffect` (frame callback + delay loop) survives the NavHost composable lifecycle correctly.

## Open Questions

- [ ] Does a `VaultHomeScreen` exist or does one need to be stubbed? (The vault route needs a composable — currently no file found.)
- [ ] Spec says `tapSequenceTimeoutMs=2000` but user notes 1.5s — confirm the tap-sequence window value.
- [ ] Spec says `swipeMinLengthDp=120dp` but user notes 100px — confirm unit and value (dp vs px matters for multi-density).
- [ ] Clock `LaunchedEffect` runs an infinite loop — verify it suspends/resumes correctly inside NavHost when clock route is popped.
- [ ] Confirm auth route animation: should the clock exit animate (fade out) simultaneously with auth enter (scale+fade in)?
