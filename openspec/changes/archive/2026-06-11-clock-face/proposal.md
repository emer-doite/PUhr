# Proposal: Clock Face — Primary Disguise Layer

## Intent

Primary disguise layer for PUhr ("Timely"). Empty `Surface` currently — no clock UI means the disguise fails. This change establishes a convincing analog+digital clock face.

## Scope

### In Scope
- Canvas analog clock with smooth sweep second hand
- Digital HH:MM:SS readout (system monospace)
- Full-screen portrait, edge-to-edge
- Time via `LaunchedEffect` + `withFrameMillis`/`delay(1000)`
- New `PUhr/clock/` package: ClockScreen, AnalogClockFace, ClockViewModel
- Portrait lock in manifest
- Blueprint §11.2 Mode A hierarchy

### Out of Scope
- Binary/zen modes
- Custom fonts
- Alarms, notifications, triggers
- AOD, orientation handling
- Gesture handler (stub only)
- Mode switching

## Capabilities

### New Capabilities
- `clock-face`: Real-time analog + digital clock. MUST show current time with smooth sweep, MUST render full-screen portrait, MUST update via LaunchedEffect.

### Modified Capabilities
None.

## Approach

Analog via Compose `Canvas` `DrawScope` (circles + `rotate` for hands), digital overlay via `Text` with `FontFamily.Monospace`. Time from `kotlinx.datetime.Clock.System.now()` inside `LaunchedEffect`. Smooth sweep via `withFrameMillis` at 60fps; digital at 1s via `delay(1000)`. `ClockViewModel` holds mode state only. Full-screen with `enableEdgeToEdge()` + `Modifier.windowInsetsPadding()`.

## Affected Areas

| Area | Impact |
|------|--------|
| `MainActivity.kt` | Modified — enableEdgeToEdge, host ClockScreen |
| `AndroidManifest.xml` | Modified — portrait lock |
| `PUhr/clock/ClockScreen.kt` | New |
| `PUhr/clock/AnalogClockFace.kt` | New |
| `PUhr/clock/ClockViewModel.kt` | New |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Battery from 60fps sweep | Med | `derivedStateOf` + Canvas; stop when not visible |
| Canvas untestable | High | Manual verification; keep drawing logic pure |
| Cutout overlap from insets | Low | Apply `systemBars` windowInsetsPadding |
| Deprecated screenOrientation | Low | Use manifest attr; migrate if needed |

## Rollback Plan

1. Delete `app/src/main/java/PUhr/clock/`
2. Revert `MainActivity.kt` to empty Surface
3. Revert manifest — remove `screenOrientation`
4. Verify `./gradlew assembleDebug`

## Dependencies

None. All APIs in existing Compose BOM.

## Success Criteria

- [ ] `./gradlew assembleDebug` compiles
- [ ] Full-screen clock shows correct time
- [ ] Second hand sweeps smoothly
- [ ] Digital readout updates every 1s
- [ ] No overlap with status bar/cutout
- [ ] Portrait lock enforced
