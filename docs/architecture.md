# Architecture

The application originated as individual Java/SwiftBot coursework (2022–23). The repository version preserves the robot-based measurement concept while isolating calculation and hardware interaction for offline tests.

## Processing flow

1. `ConsoleInputHandler` confirms calibration and the start of the run; `ConsoleMessages` displays instructions.
2. `UltrasonicCalibration` reads the initial distance, moves towards a reference object using the original `40,40` wheel commands, and derives estimated speed from change in distance over elapsed time. It validates readings and limits sample/time budgets *between returned API calls*.
3. `TunnelVisionApp` starts the journey inside the first tunnel; `SwiftBotRobotAdapter` provides access to `SwiftBotAPI` through `RobotPort`.
4. `CameraFrameProcessor` receives an image from the SwiftBot adapter; `ImageIntensityAnalyzer` calculates brightness across pixels.
5. `TunnelStateDetector` detects candidate transitions from image brightness changes and stabilisation, alternating tunnel and gap states. A lost frame invalidates pending detection.
6. `SegmentTimer` measures completed segments. `DistanceChecks` calculates estimated lengths and validates coursework minimums/variation.
7. `JourneyReport` prepares the text output; `JourneyLogWriter` prints it and, if requested, appends it to `LOG.txt`.
8. `RobotSafety` attempts to stop the wheels, disable LEDs and shut down, including on abnormal exits.

## Main Java components

| Responsibility | Implementation |
|---|---|
| Application orchestration | `TunnelVisionApp` |
| SwiftBot API boundary | `RobotPort`, `SwiftBotRobotAdapter` |
| Ultrasonic calibration | `UltrasonicCalibration` |
| Image acquisition and failures | `CameraFrameProcessor`, `CameraFailurePolicy` |
| Grayscale and transition detection | `ImageIntensityAnalyzer`, `TunnelStateDetector` |
| Timing and validation | `SegmentTimer`, `DistanceChecks` |
| Console interaction | `ConsoleInputHandler`, `ConsoleMessages` |
| Journey reporting | `JourneyReport`, `JourneyLogWriter` |
| Path and failure settings | `AppConfig` |
| Best-effort cleanup | `RobotSafety` |

## Measurement assumptions

Estimated lengths are calculated from a single calibrated speed and segment duration. The transition detector uses intensity thresholds drawn from the original coursework; these thresholds do not guarantee accurate physical transitions in different lighting. Only completed segments are included in totals. No real-robot claims are inferred from mock tests.
