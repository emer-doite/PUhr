## Exploration: Hidden Entry Mechanism

### Answers to Questions

1. **What hidden entry method does plam.md specify?** Compound gesture sequence: triple-tap center (±30dp), swipe LEFT (min 120dp, within 1.5s), long-press 6 o'clock (hold 800ms, ±40dp). All steps must complete within 8s. Alternative backup methods: dial code (`*#XXXX#`), calculator disguise, volume button sequence.

2. **How does the entry point stay undiscoverable?** Zero visual feedback on any gesture. Silent reset on wrong step — no error, no animation, no haptic. No hints in strings.xml or resources (all strings are neutral clock-related). The gesture is a specific compound sequence that cannot be stumbled upon accidentally. ProGuard/R8 aggressively obfuscates the code.

3. **What happens after entry triggered?** `SecretGestureDetector.onTriggerDetected()` callback fires. Currently no navigation framework exists — `MainActivity` directly renders `ClockScreen()` with no NavHost. The trigger should navigate to `AuthScreen` which already exists with `onVerified` callback.

4. **Should clock keep running during auth/vault?** Yes. The alarm system uses `AlarmManager` independent of app lifecycle. ClockScreen can be stopped or kept alive behind the auth/vault screens. The nav graph shows `ClockScreen` as start, with hidden routes to `AuthScreen → VaultHomeScreen`. Alarms fire even when vault is open (section 5.2).

5. **Anti-forensics: does the entry mechanism leave traces?**
   - Touch events: Android does not expose touch gesture logs to apps or ADB. The gesture detector operates entirely in-process with no logging.
   - Accessibility events: Compose gesture handlers (pointerInput) do not emit accessibility events by default unless semantic modifiers are added. Current code has no semantics on clock face.
   - App switcher: vault screens use FLAG_SECURE and custom task description. Clock screen currently does not apply FLAG_SECURE (since it's public face).
   - No network permission means no exfiltration.

### Current State

- `MainActivity.kt` directly renders `ClockScreen()` — no navigation framework
- `ClockScreen.kt` renders `AnalogClockFace` (Canvas) + digital time — **no gesture handling at all yet**
- `AnalogClockFace.kt` is a pure Canvas composable — no pointer input modifiers
- `AuthScreen.kt` exists with `onVerified: (ByteArray) -> Unit` callback, fully functional
- No `SecretGestureDetector.kt` file exists yet
- No `gesture/` package under `clock/` exists yet
- `ClockViewModel.kt` is minimal — only has mode state, no gesture event handling
- No NavHost, no navigation graph, no routing between ClockScreen and AuthScreen

### Affected Areas
- `app/src/main/java/PUhr/clock/ClockScreen.kt` — needs pointer input and gesture detection
- `app/src/main/java/PUhr/clock/AnalogClockFace.kt` — may need position detection for tap/long-press coordinates
- `app/src/main/java/PUhr/clock/gesture/SecretGestureDetector.kt` — new file (as per plam.md structure)
- `app/src/main/java/PUhr/clock/ClockViewModel.kt` — may need gesture event relay or state
- `app/src/main/java/PUhr/MainActivity.kt` — needs NavHost to switch between ClockScreen and AuthScreen
- `app/src/main/java/PUhr/auth/AuthViewModel.kt` — no changes expected (already complete)
- `AndroidManifest.xml` — vault activities need `excludeFromRecents="true"` (already in plam.md template)
- `openspec/specs/` — may need a `hidden-entry/spec.md` later

### Approaches

1. **Compose-only gesture detection (recommended)**
   - Use `Modifier.pointerInput` on the clock face composable to detect taps, swipes, long-presses
   - `detectTapGestures` for triple-tap (count taps within time window)
   - `detectDragGestures` for swipe direction
   - `detectTapGestures(onLongPress = ...)` for long-press at specific position
   - Pros: Fully in-Compose, no Android view interop, lifecycle-aware, testable with Compose test framework
   - Cons: Triple-tap detection requires manual time-window tracking (Compose doesn't have built-in triple-tap)
   - Effort: Medium

2. **Android View `GestureDetector` + interop**
   - Wrap the clock face in `AndroidView` and use platform `GestureDetector` / `ScaleGestureDetector`
   - Pros: Battle-tested gesture APIs, built-in triple-tap (`setOnTripleTapListener`?), multi-touch
   - Cons: View-Compose interop adds complexity, breaks pure Compose architecture, harder to test with ComposeTestRule
   - Effort: Medium

3. **`Modifier.pointerInput` with custom `AwaitPointerEventScope`**
   - Build a custom gesture state machine using `awaitPointerEvent`, `awaitFirstDown`, `waitForUpOrCancellation`
   - Pros: Full control over gesture pipeline, can handle the compound sequence as one atomic state machine, no intermediate callbacks
   - Cons: More code, more complex, easier to miss edge cases
   - Effort: High

### Recommendation

**Approach 1 (Compose-only pointerInput)** with Manual step tracking using `detectTapGestures` for the tap and long-press parts, and `detectHorizontalDragGestures` for the swipe. The compound sequence state machine lives in a dedicated `SecretGestureDetector` class (as per plam.md design) that the composable calls from pointerInput lambdas.

### Key Decisions
- **Navigation**: Needs a `NavHost` in `MainActivity` — clock is always start destination, auth/vault routes are hidden (not in back stack preview)
- **Back stack**: Clock screen should be popped from back stack when entering auth/vault to prevent back-navigation revealing vault. Use `composable(route) { ... }` with `popUpTo(CLOCK_ROUTE) { inclusive = true }`
- **Gesture scope**: Attach `pointerInput` to the full `ClockScreen` Column, not only the clock face Canvas — gives more touch area for gesture detection
- **State machine**: `SecretGestureDetector` should be a plain Kotlin class injected or instantiated in the composable via `remember`, not in ViewModel (gesture state is UI-only, no business logic)

### Open Questions
1. Should the clock continue ticking in the background when the vault is open? (plam.md suggests yes for plausibility — alarm in notification bar still shows "Timely")
2. Is a NavHost transition or a simple visibility toggle (`if (showAuth) AuthScreen() else ClockScreen()`) preferred? The latter is simpler but doesn't use the standard navigation graph.
3. Should the zoom animation (swipe-to-auth) be implemented now or in a later phase?
4. What is the minimum SDK requirement — `pointerInput` works API 21+ but `detectTapGestures` multi-tap behavior varies. plam.md says API 28+.
5. Should the swipe in step 2 cancel immediately if the swipe is not horizontal-left, or should it only validate at the end of the drag? (plam.md: "swipe LEFT across the clock face")

### Risks
- **Accidental trigger**: The 8-second window and specific combination (triple-tap + swipe + long-press at exact position) make accidental trigger statistically improbable, but edge cases with rapid gesture spam should be tested
- **Accessibility**: If a user relies on TalkBack, the gesture sequence may conflict with accessibility gestures. Consider dial-code backup method for these users (listed in plam.md section 6.3)
- **Screen size variance**: Position tolerances (±30dp center, ±40dp 6-o'clock) need testing across screen sizes and densities
- **Navigation framework**: No NavHost exists yet — needs to be introduced which is a separate concern

### Ready for Proposal
Yes. The approach is well-understood, the spec is clear from plam.md, and the codebase is ready.
