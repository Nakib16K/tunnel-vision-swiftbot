# Tunnel Vision — SwiftBot Tunnel & Gap Measurement

**Individual project · Brunel University London · 2022–23**
**Java · SwiftBot API · Image processing · Ultrasonic calibration · Automated tests**

Tunnel Vision is a Java application developed as an **individual university project** to estimate the lengths of tunnels and the gaps between them using a SwiftBot robot. It combines camera-based grayscale intensity changes with ultrasonic speed calibration, timed motion segments, command-line prompts and journey logs.

The repository presents the original project concept alongside software refinements undertaken **after evaluation of the coursework**. Those refinements are not represented as part of the original submission or as physically retested robot behaviour.

## What it does

- Calibrates an estimated forward speed using ultrasonic distance measurements.
- Detects changes between darker tunnel interiors and brighter gaps using grayscale camera images.
- Measures completed tunnel and gap segments from their elapsed duration and the calibrated speed.
- Checks the coursework constraints: **tunnel length ≥30 cm**, **gap length ≥20 cm**, and **10–15 cm** difference between consecutive tunnel lengths.
- Displays progress in the console and, when requested, appends measurements, total distance, duration and length extrema to a text log.
- Attempts motor stop, LED disable and shutdown when the application exits or encounters a recoverable error.

**Measurement model:** `estimated distance (cm) = calibrated speed (cm/s) × elapsed time (s)`.
This is a time-and-speed estimate, not a guarantee of actual distance on a physical robot.

## How it works

```text
Console confirmation → ultrasonic calibration → start in first tunnel
                                             ↓
                           camera grayscale intensity samples
                                             ↓
                   tunnel/gap transition detection + segment timing
                                             ↓
                         length checks + optional journey log
                                             ↓
                               stop and shutdown attempt
```

The robot begins in the **first tunnel**; tunnel-to-gap and gap-to-tunnel events alternate. Only completed segments contribute to the report. The application instructs the operator to arrange at least three tunnels, but **does not enforce three completed measurements** when stopping.

See [Architecture](docs/architecture.md) for class responsibilities and [Evaluation and improvements](docs/evaluation-and-improvements.md) for the relationship between the coursework and this repository version.

## Build and test

The build uses **Java 11 source/bytecode compatibility** and Apache Ant. The desktop verification was performed with a newer JDK compiling via `--release 11`; a Java 11 runtime was not separately checked.

1. Obtain a **legitimate, compatible copy** of `SwiftBotAPI-4.0.2.jar` and place it at `lib/SwiftBotAPI-4.0.2.jar`. The vendor JAR is not included here; see [`lib/README.md`](lib/README.md).
2. From the project root, run:

```bash
ant clean test   # compiles source and runs offline verification
ant compile      # builds production Java sources only
```

The tests simulate images, sensor values and hardware interactions. They **do not launch the real robot application or contact a Raspberry Pi**. No external-network or remote deployment targets are provided.

To open in Eclipse, use **File → Import → Existing Projects into Workspace**, attach the local JAR if necessary and select a suitable JDK. The entry point is `io.github.nakib16k.tunnelvision.TunnelVisionApp`. The Eclipse configuration has been inspected structurally, but a GUI import was not tested.

**Do not run `main()` on an ordinary desktop expecting the SwiftBot to be simulated.** The real entry point instantiates a hardware-specific API adapter. Use `ant test` for desktop verification.

## Validation

The submitted **2023 manual functional test record** contains **32 cases: 31 passed and one failed**. The failed case documented the SwiftBot curving right instead of moving straight; the original motor steering values have been preserved rather than claiming that this was corrected.

The repository also includes **214 passing desktop test assertions in 25 groups** covering core logic, failure handling and regression checks. Those checks use mocks and synthetic inputs; they are **not additional on-device test cases**. A local clean-build of this package is described in [Testing](docs/testing.md).

## Configuration and outputs

The program saves session images and optionally appends `LOG.txt` inside a configured data directory. Set `-Dtunnelvision.dataDir=/path/to/data` as a JVM property, or provide the `TUNNEL_VISION_DATA_DIR` environment variable. If neither is set, the default is `/home/pi/Documents`, the location used in the coursework. Output files and vendor JARs are ignored by Git.

## Repository layout

```text
src/io/github/nakib16k/tunnelvision/   Java application and testable components
test/io/github/nakib16k/tunnelvision/  CoreLogicTests, ReliabilityTests, IndependentRegressionTests
docs/                                  Architecture, testing, evaluation and limitations
lib/README.md                          Local vendor JAR instructions
build.xml                               Java 11-targeted local Ant build and tests
.project, .classpath, .settings/       Eclipse configuration
.gitignore                              Generated files and local dependencies
```

## Known limitations

Physical SwiftBot and Raspberry Pi hardware are unavailable. Camera accuracy, ultrasonic response, actual motion, emergency stopping and the effectiveness of calibration safeguards **have not been retested**. Software polling limits cannot interrupt a vendor method that blocks indefinitely. See [Known limitations](docs/known-limitations.md) for a complete list.

## License

The original project code and repository-authored documentation are available under the [MIT License](LICENSE) (Copyright © 2023 Abdullah Al Nakib). The license **does not apply** to the third-party SwiftBot API or any Brunel University course materials. The API JAR and unredacted coursework PDFs are not distributed in this repository.

**Project scope:** Individually authored coursework; not an official SwiftBot or Brunel University project.
