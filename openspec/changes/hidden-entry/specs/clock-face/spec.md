# Clock Face — Hidden Entry Delta Spec

## Purpose

Add the gesture integration surface on ClockScreen for the hidden vault entry mechanism. The `AnalogClockFace` composable remains unchanged — gesture detection runs on the enclosing `Column`.

## Spec Changes

### New Requirement: Gesture Integration Surface (REQ-CLOCK-06)

`ClockScreen` MUST accept an `onTriggerDetected: () -> Unit` callback parameter. The `ClockScreen` `Column` MUST apply a `Modifier.pointerInput` for gesture detection. The `Column` SHALL be the gesture container — `AnalogClockFace` and the digital readout render as children without pointer input modifications. The clock's visual appearance MUST remain identical with or without gesture detection active.

#### Scenario: ClockScreen accepts onTriggerDetected callback

- GIVEN ClockScreen is composable
- WHEN it is invoked with an `onTriggerDetected` lambda
- THEN the lambda is called when the compound gesture sequence completes

#### Scenario: Clock appearance unchanged with gesture

- GIVEN ClockScreen renders with gesture detection
- WHEN the clock face and digital readout are inspected
- THEN they appear identical to a ClockScreen without gesture detection

#### Scenario: Gesture events do not interfere with clock rendering

- GIVEN ClockScreen is rendering with gesture detection
- WHEN the user taps anywhere on the clock screen
- THEN the gesture detector receives the event
- AND the clock rendering is visually unaffected
