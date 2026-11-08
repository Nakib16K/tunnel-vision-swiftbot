# Testing and verification

## Original evaluation — 2023

The submitted implementation report documented **32 manual functional cases**: **31 passed, 1 failed**. The failed case concerned the SwiftBot curving right rather than travelling in a straight line. This result is not superseded by the software-only tests. Historical records are held separately and are not bundled into the public repository.

## Desktop tests for the evaluated project

The repository has three standalone Java test entry points (no JUnit dependency required):

| Suite | Assertions | Coverage |
|---|---:|---|
| `CoreLogicTests` | 96 | Grayscale image arithmetic, input prompts, timers, tunnel/gap checks, report generation, state transitions and simulated journeys |
| `ReliabilityTests` | 81 | Output paths, camera errors, invalid ultrasonic readings, calibration bounds, logging failures and cleanup |
| `IndependentRegressionTests` | 37 | Negative paths, rejected gaps, shutdown failures, interrupted calibration and original prompt/motor parameter regression checks |
| **Total** | **214** | **25 test groups; zero failures in the clean desktop verification** |

The suite runs with `ant clean test` after placing an appropriately obtained `SwiftBotAPI-4.0.2.jar` in `lib/`. All tests use simulated inputs and stubs; **the real hardware entry point is never launched**. The clean extraction was built with OpenJDK 21 using Java 11-target compiler settings (`--release 11`) and Apache Ant. Separate Java 11 runtime execution and Eclipse GUI import were not checked.

## Verification boundaries

Passing desktop tests provides evidence for software calculations and the tested failure paths only. It does **not** verify physical tunnel-length precision, ultrasonic calibration accuracy, image acquisition latency, steering, LED behaviour, safe stopping, or behaviour if vendor API calls never return.

The provided vendor JAR is a local build dependency and is excluded from the repository. The optional cloud CI workflow is not configured because a confirmed lawful automated vendor-JAR download route is not established.
