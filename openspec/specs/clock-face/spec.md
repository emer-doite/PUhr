# Clock Face Specification

## Purpose

Define the clock face — the primary disguise layer for PUhr ("Timely"). The clock MUST present a convincing real-time analog + digital clock display hiding the vault entry surface.

## Requirements

### Requirement: Analog Clock Display (REQ-CLOCK-01)

The system MUST render an analog clock face using Canvas-based Compose drawing. The clock face MUST show hour, minute, and second hands. The second hand SHOULD sweep smoothly at 60fps rather than discrete tick-tock movement.

#### Scenario: Clock shows correct hand angles at 3:00:00

- GIVEN the system clock reads 3:00:00
- WHEN the analog clock renders
- THEN the hour hand points at 90 degrees
- AND the minute hand points at 0 degrees
- AND the second hand points at 0 degrees

#### Scenario: Second hand sweeps between marks

- GIVEN the analog clock is rendering
- WHEN 250ms have elapsed since the last second boundary
- THEN the second hand position MUST be between two consecutive second marks
- AND NOT at a discrete second boundary

### Requirement: Digital Readout (REQ-CLOCK-02)

The system MUST display HH:MM:SS below the analog clock using monospace font. The digital readout MUST update every 1 second. The readout MUST NOT show the date.

#### Scenario: Digital readout matches system time

- GIVEN the system clock reads 14:30:45
- WHEN the digital readout renders
- THEN it displays "14:30:45"

#### Scenario: Readout updates on second boundary

- GIVEN the readout shows "14:30:45"
- WHEN exactly 1 second elapses
- THEN the readout shows "14:30:46"

#### Scenario: Midnight rollover

- GIVEN the readout shows "23:59:59"
- WHEN exactly 1 second elapses
- THEN the readout shows "00:00:00"

### Requirement: Screen Layout (REQ-CLOCK-03)

The system MUST render full-screen with edge-to-edge mode behind system bars. The clock MUST apply windowInsetsPadding to avoid overlap with cutouts, status bar, and navigation bar. The screen SHOULD lock to portrait orientation.

#### Scenario: Clock avoids system bar overlap

- GIVEN the app renders in edge-to-edge mode
- WHEN the clock layout is measured
- THEN clock content MUST NOT overlap with status bar or navigation bar

#### Scenario: Portrait lock enforced

- GIVEN the device is rotated to landscape
- WHEN the manifest declares screenOrientation="portrait"
- THEN the display remains in portrait orientation

### Requirement: Time Updates (REQ-CLOCK-04)

The system MUST read time from the system clock. The analog second hand SHOULD use LaunchedEffect with withFrameMillis for smooth animation. The system MUST NOT modify system time or timezone. The system MUST NOT trigger alarms or notifications.

#### Scenario: Time reflects system clock

- GIVEN the system clock changes via NTP sync
- WHEN the clock face renders
- THEN it reflects the updated system time

#### Scenario: Animation stops in background

- GIVEN the app is sent to the background
- WHEN the activity is not visible
- THEN the LaunchedEffect coroutine cancels
- AND no frame callbacks fire

### Requirement: Disguise Integrity (REQ-CLOCK-05)

The system SHALL NOT show any vault-related UI elements. The system SHALL NOT expose vault entry points. The clock MUST appear as a standard clock application.

#### Scenario: No vault UI visible

- GIVEN the clock face is displayed
- THEN there MUST be no text, icon, or button referencing "vault", "locker", "safe", or "encrypt"

#### Scenario: Single-purpose appearance

- GIVEN a user launches the app
- WHEN the clock face renders
- THEN it displays ONLY clock-related content
- AND no secondary UI elements beyond the clock

### Requirement: Gesture Integration Surface (REQ-CLOCK-06)

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
