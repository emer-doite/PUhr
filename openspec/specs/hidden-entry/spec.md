# Hidden Entry Specification

## Purpose

Define the hidden vault entry mechanism — a compound gesture sequence that navigates from the clock face to PIN auth without exposing any vault-related UI or leaving forensic traces.

## Requirements

### Requirement: Gesture State Machine (REQ-HIDE-01)

The system MUST implement an implicit state machine with states `idle → threeTaps → swiped → longPressed → triggered`. The machine MUST reset to `idle` on any deviation from the expected sequence, timeout expiry, or wrong position. The reset MUST be silent — no callback, no logging. The state machine is implemented as a sequential pipeline within `awaitEachGesture`; no explicit `GestureStep` sealed class is required.

#### Scenario: Full gesture sequence triggers successfully

- GIVEN SecretGestureDetector is in `idle` state
- WHEN the user performs 3 taps within ±30dp of center, each ≤300ms apart, all 3 within 2s
- AND then swipes left ≥120dp within 1.5s
- AND then long-presses in the lower 30% of screen for ≥800ms
- AND total elapsed time from first tap is ≤8s
- THEN `onTriggerDetected` fires
- AND state becomes `triggered`

#### Scenario: Wrong step resets silently

- GIVEN SecretGestureDetector is in `tapped3` state
- WHEN the user swipes right instead of left
- THEN state resets to `idle`
- AND no callback fires

#### Scenario: Timeout resets silently

- GIVEN SecretGestureDetector is in `swiped` state
- WHEN 8s elapses from first tap without completing the sequence
- THEN state resets to `idle`
- AND no callback fires

#### Scenario: Wrong position resets silently

- GIVEN SecretGestureDetector is in `idle` state
- WHEN the user taps at coordinates outside ±30dp of center
- THEN the tap is ignored
- AND state remains `idle`

#### Scenario: Second tap outside center tolerance resets

- GIVEN SecretGestureDetector is in `tapped` state after 1 center tap
- WHEN the user taps a second time at coordinates >30dp from center
- THEN state resets to `idle`
- AND no callback fires

### Requirement: Gesture Parameters (REQ-HIDE-02)

Gesture parameters MUST be configurable via a `GestureConfig` data class with these defaults:

| Parameter | Default | Description |
|---|---|---|
| tapToleranceDp | 30dp | Max distance from center for step 1 taps |
| swipeMinLengthDp | 120dp | Minimum swipe travel for step 2 |
| swipeTimeoutMs | 1500 | Max time after step 1 to start swipe |
| longPressLowerFraction | 0.3 | Fraction of screen height that qualifies as lower zone |
| longPressHoldMs | 800 | Hold duration for step 3 |
| totalTimeoutMs | 8000 | Max total time from first tap |
| tapSequenceTimeoutMs | 2000 | Max time to complete all 3 taps |
| tapMaxIntervalMs | 300 | Max gap between consecutive taps |

#### Scenario: Config overrides change behavior

- GIVEN a SecretGestureDetector with `tapToleranceDp=50dp`
- WHEN the user taps at 40dp from center
- THEN the tap is accepted
- AND state advances

### Requirement: Anti-Forensics (REQ-HIDE-03)

SecretGestureDetector MUST NOT log any gesture events, state transitions, or timeouts. The gesture area MUST NOT have semantic modifiers or accessibility labels. The system MUST NOT emit any visual, audio, or haptic feedback during any step. On timeout or wrong step, the system MUST reset silently with no observable side effect.

#### Scenario: No logging on any gesture event

- GIVEN a SecretGestureDetector instance
- WHEN any gesture event occurs (tap, swipe, long-press, timeout, reset)
- THEN no logcat output is produced by SecretGestureDetector

#### Scenario: No semantics on gesture container

- GIVEN the ClockScreen composable with pointerInput modifier
- WHEN the accessibility tree is inspected
- THEN no semantic modifiers or content descriptions are present on the gesture container

#### Scenario: No visual feedback on correct step

- GIVEN the user has completed step 1 (triple-tap)
- WHEN the state machine advances to `tapped3`
- THEN the clock face appearance is unchanged
- AND no animation, color change, or indicator appears

#### Scenario: Silent reset has no observable effect

- GIVEN SecretGestureDetector is in `swiped` state
- WHEN a timeout occurs
- THEN the clock face remains in its current state
- AND no UI update is triggered
- AND no haptic or audio feedback occurs

### Requirement: Navigation Integration (REQ-HIDE-04)

MainActivity MUST replace direct `ClockScreen()` rendering with a `NavHost`. The NavHost MUST define routes: `"clock"`, `"auth"`, `"vault"`. The clock route MUST be the start destination. Auth and vault routes MUST NOT be visible in nav graph preview. Navigating to auth MUST pop the clock route from the back stack using `popUpTo("clock") { inclusive = true }`.

#### Scenario: NavHost renders clock as start

- GIVEN the app launches
- WHEN MainActivity.onCreate runs
- THEN NavHost is rendered with startDestination="clock"
- AND the clock composable is visible
- AND auth and vault composables are not rendered

#### Scenario: Trigger navigates to auth with clean back stack

- GIVEN NavHost is displaying the clock route
- WHEN `onTriggerDetected` fires
- THEN the app navigates to "auth"
- AND the clock route is removed from the back stack
- AND pressing back exits the app

#### Scenario: Auth to vault preserves clean back stack

- GIVEN the user is on the auth route
- WHEN authentication succeeds
- THEN navigation to "vault" pops the auth route
- AND vault is the only route in the back stack
- AND pressing back exits the app

#### Scenario: Auth and vault routes hidden from nav preview

- GIVEN the NavHost composable is rendered
- WHEN the navigation graph is inspected via developer tools
- THEN the "auth" and "vault" routes are not visible

### Requirement: Trigger Animation (REQ-HIDE-05)

On navigation to the auth route, the system MUST play a scale-fade enter transition. The animation MUST complete within 400ms. The transition SHOULD be subtle — scale from 0.95 to 1.0 combined with fade-in from 0 to 1 alpha.

#### Scenario: Scale-fade plays on auth enter

- GIVEN the user triggers the gesture sequence
- WHEN navigation to auth begins
- THEN the auth screen enters with a scale-fade animation
- AND the animation completes within 400ms

### Requirement: Package Structure (REQ-HIDE-06)

The system MUST create a package `PUhr.clock.gesture` containing:
- `SecretGestureDetector` — Modifier extension function implementing the gesture pipeline (Compose types are acceptable since this is a UI modifier)
- `GestureConfig` — data class for configurable parameters (plain Kotlin)

The `VaultHomeScreen` composable MUST live in `PUhr.vault` package.

#### Scenario: Package contains required types

- GIVEN the `PUhr.clock.gesture` package
- THEN it MUST contain `SecretGestureDetector` and `GestureConfig`
- AND `VaultHomeScreen` MUST be in `PUhr.vault`
