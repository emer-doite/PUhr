# Proposal: Hidden Entry Mechanism

## In Scope

| Item | Priority | Effort |
|---|---|---|
| **SecretGestureDetector state machine** | P0 | M |
| `idle -> tapped3 -> swiped -> longPressed -> triggered` — plain Kotlin class, no logging, no visual feedback, 8s timeout reset | | |
| **NavHost scaffold in MainActivity** | P0 | S |
| Replace direct `ClockScreen()` with `NavHost(startDestination = "clock")`. Routes: `clock`, `auth`, `vault`. No routes visible in nav graph preview. | | |
| **Gesture integration on ClockScreen** | P0 | M |
| `Modifier.pointerInput` on `ClockScreen` Column. Triple-tap via manual tap-count + time window, horizontal drag via `detectHorizontalDragGestures`, long-press via `detectTapGestures(onLongPress)`. Position checks for center-tap (±30dp) and 6-o'clock (±40dp). | | |
| **Anti-forensics** | P0 | S |
| Zero logging in SecretGestureDetector. No semantics/accessibility hints on gesture area. No visual feedback on any step. Silent reset on timeout or wrong sequence. | | |
| **Trigger animation** | P1 | S |
| Subtle zoom/fade transition via `AnimatedNavHost` or `crossfade` on trigger. | | |
| **Back stack cleanup** | P0 | S |
| `popUpTo("clock") { inclusive = true }` when navigating to `auth` — no back-navigation to clock from vault. | | |
| **Gesture/ package** | P0 | S |
| New package `PUhr.clock.gesture` with `SecretGestureDetector.kt` and gesture model types. | | |

## Out of Scope

| Item | Reasoning |
|---|---|
| Dial-code backup entry (plam.md §6.3) | Separate feature, deferred to later change |
| Zoom/swipe animation polish | P1 — basic transition in scope, visual polish deferred |
| Vault browse UI | Already separate — vault-home stub exists but no browse UI |
| Biometric auth | Already implemented in AuthScreen, not part of this change |
| Clock timer/alarm features | Unrelated to entry mechanism |

## Approach

### Architecture

```
┌──────────────────────────────────────┐
│  MainActivity                        │
│  ┌────────────────────────────────┐  │
│  │  NavHost (start = "clock")     │  │
│  │  ┌─────────┐ ┌──────┐ ┌─────┐ │  │
│  │  │ Clock   │→│ Auth │→│Vault│ │  │
│  │  │ Screen  │ │Screen│ │Home │ │  │
│  │  └─────────┘ └──────┘ └─────┘ │  │
│  └────────────────────────────────┘  │
└──────────────────────────────────────┘
```

- **SecretGestureDetector** — plain Kotlin class, `remember`'d in ClockScreen composable. Holds step index, timeout coroutine job. Receives gesture events, advances or resets. Fires `onTriggerDetected` lambda.
- **Gesture detection** — `Modifier.pointerInput` on the ClockScreen Column. Uses `awaitPointerEventScope` directly (not `detectTapGestures` alone) because the compound sequence needs to own the full event pipeline across steps.
- **Navigation** — `NavHost` replaces direct composable rendering. `clock` route renders `ClockScreen` with an `onTriggerDetected` callback that navigates to `auth`. Auth route gets `popUpTo("clock") { inclusive = true }` in the `navToAuth` call.
- **Animation** — `AnimatedNavHost` with a subtle scale+fade enter/exit transition on the auth route.

### Key Design Decisions

| Decision | Choice | Rationale |
|---|---|---|
| State location | Composable-local `remember`, not ViewModel | Gesture state is purely UI — no business logic, no reason to live in ViewModel |
| Gesture API | `awaitPointerEventScope` raw mode (Approach 3 from exploration) | Compound sequence needs to own the event pipeline; split gesture detectors (`detectTapGestures` + `detectHorizontalDragGestures`) cannot coordinate across steps cleanly |
| Nav or visibility toggle | `NavHost` | Standard pattern, supports back stack management, `AnimatedNavHost` for transitions |
| Clock behind vault | Not rendered | `popUpTo(inclusive=true)` removes clock from back stack entirely; clock is effectively stopped. If background clock needed — separate concern for later |
| Animation trigger | Nav argument `?triggered=true` on auth route | Allows ClockScreen to play exit animation before nav completes; `AnimatedNavHost` reads the arg for enter transition |

## Risks

| Risk | Mitigation |
|---|---|
| **Accidental trigger** | 8s window + 3 distinct gesture types + position tolerances make it statistically ~impossible. Edge case: rapid random tapping. Test with monkey runner. |
| **TalkBack conflict** | Compound gesture will conflict with accessibility. Documented. Dial-code backup (deferred) addresses this. |
| **Screen size variance** | Tolerance values (30dp, 40dp) work across densities but need testing on tablets and small phones. Parameterize in SecretGestureDetector config. |
| **NavHost refactor** | Changing `MainActivity` from direct composable to NavHost affects the composable tree root. Verify no recomposition issues with the always-running clock LaunchedEffect. |
| **Platform gesture conflict** | System back gesture, notification pull-down, edge-to-edge swipe from `enableEdgeToEdge()`. Test that edge swipes don't interfere with clock-face gesture zone. |

## Next Step

**sdd-spec**: Write delta specs with requirements and scenarios covering:
- Compound gesture sequence acceptance criteria
- SecretGestureDetector unit tests (step advancement, timeout, reset, trigger)
- NavHost integration test (clock → auth → vault, back behavior)
- Anti-forensics verification (no logs, no semantics, no visual feedback)
