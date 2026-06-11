## Exploration: Clock Face — PUhr Disguise Layer

### Current State
The app has a complete Android scaffold (14 files) with Hilt + Compose + Material 3 dark theme, but the `Surface` in `MainActivity.kt:37` is empty. There is no clock UI, no time-tracking logic, no gesture handling — the disguise layer does not exist yet. The `plam.md` blueprint defines the clock as the primary disguise surface with three visual modes, a compound gesture trigger, and specific aesthetic tokens.

### Affected Areas
- `app/src/main/java/PUhr/MainActivity.kt` — entry point; will need to host `ClockScreen`
- `app/src/main/java/PUhr/presentation/clock/` — new package for clock UI (to be created)
- `gradle/libs.versions.toml` — may need font library dependency
- `app/src/main/res/font/` — may bundle custom typefaces
- `app/src/main/res/values/strings.xml` — clock-related strings

### Approaches

1. **Analog-only (Canvas-based) — MVP**
   - One composable: `AnalogClockFace` using Compose `Canvas` + `DrawScope` for hands, dial, and smooth sweep
   - Time via `LaunchedEffect` + `withFrameMillis` for smooth second hand animation
   - Pros: Looks like a real clock app immediately; smooth sweep is visually convincing; minimal code (~150 lines); Canvas render is efficient
   - Cons: Only Mode A of the blueprint; no binary/zen toggle yet; custom fonts deferred
   - Effort: Low

2. **Digital-only (Text-based) — Quickest path**
   - `Text` composable with `remember` + `LaunchedEffect` updating every second
   - Pros: Trivial to implement (~30 lines); works immediately
   - Cons: Does NOT look like a real clock app (most clock apps show analog); weak disguise; no Canvas practice for later modes
   - Effort: Very Low

3. **Analog + Digital readout — Blueprint-faithful default**
   - Analog clock face (Canvas) as the primary visual + digital HH:MM:SS text overlay at bottom
   - Follows blueprint §11.2 visual hierarchy exactly
   - Pros: Closest to blueprint; strongest disguise (real clock look); digital readout reinforces realism; straightforward to extend with binary/zen modes later
   - Cons: Slightly more code than analog-only; still defers binary/zen toggle
   - Effort: Medium

4. **All three modes at once — Full blueprint scope**
   - Implement Mode A (analog), Mode B (digital+binary with BCD LED rows), Mode C (zen minimal) in one change
   - Pros: No deferred work; complete disguise layer
   - Cons: Significantly more code (~400+ lines across multiple composables); binary BCD layout needs careful pixel math; over-scoped for a first clock change; increases review budget risk
   - Effort: High

### Recommendation

**Approach 3: Analog + Digital readout** — the blueprint-faithful default.

Rationale:
- The disguise MUST be convincing. Analog clock + digital readout is what users expect from a "Timely" clock app. Digital-only is suspicious.
- The blueprint explicitly shows this layout in §11.2: analog face centered, digital time below, then alarms.
- Canvas-based analog gives us the smooth-sweep second hand (§5.1), which is visually impressive and sells the disguise.
- Leaves binary/zen modes for a follow-up change (keeps review budget under 400 lines).
- `withFrameMillis` in `LaunchedEffect` enables sub-1000ms frame updates for smooth sweep without a ViewModel — lightweight.

**Architecture decision**: New `clock` package at `app/src/main/java/PUhr/clock/` (flattened from blueprint's `presentation/clock/` — the `presentation` layer is unnecessary indirection at this stage). Files:
- `ClockScreen.kt` — full-screen clock composable, orchestrates analog + digital + gesture handler
- `AnalogClockFace.kt` — Canvas-based analog clock with hour/minute/second hands
- `ClockViewModel.kt` — `@HiltViewModel` injecting `ClockTimeUseCase` (or inline time flow)
- `gesture/SecretGestureDetector.kt` — composite gesture detector (future; stub for now)

**Time mechanism**: `LaunchedEffect` + `withFrameMillis` for smooth second hand; fallback `LaunchedEffect` + `delay(1000)` for the digital readout. This avoids coupling to a ViewModel purely for time updates. The ViewModel is reserved for alarm data and mode state.

**Fonts**: Defer custom fonts (Space Grotesk / JetBrains Mono) to a dedicated font follow-up. Use MaterialTheme typography defaults for the digital readout — `titleLarge` or `displayLarge` with monospace appearance via `fontFamily = FontFamily.Monospace`. The analog clock uses Canvas drawing, so fonts only affect the digital text.

**Screen layout**: Full-screen with `fillMaxSize()`, `background = Color(0xFF0D0D0F)` (blueprint deep charcoal). No system bars (edge-to-edge with `enableEdgeToEdge()` in MainActivity). Portrait-locked initially (orientation handling deferred). No always-on display support yet — that's battery optimization, not disguise-critical.

### Risks
- **Smooth second hand animation**: `withFrameMillis` at 60fps could spike battery if not composed efficiently. Mitigation: use `derivedStateOf` to avoid recomposition on every frame; draw in `Canvas` which skips recomposition.
- **ClockViewModel scope**: If ViewModel is scoped to the activity, it survives config changes (good). But if we later add a real alarm feature, the ViewModel may grow too large. Mitigation: keep ViewModel minimal — time state + mode toggle only.
- **Canvas testing**: Canvas-based rendering is hard to unit test. Mitigation: this is visual code; accept lower test coverage and rely on manual verification for now.
- **Edge-to-edge**: Must call `enableEdgeToEdge()` in `MainActivity` and apply `WindowInsets` to the clock content, or the clock might render under cutouts.

### Ready for Proposal
Yes — the analog+digital approach is clear, well-scoped, and follows the blueprint. The proposal phase can refine file count, exact composable signatures, and the ViewModel contract.
