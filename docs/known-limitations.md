# Known limitations

1. **No physical hardware validation of the repository version.** A SwiftBot and Raspberry Pi are unavailable. No movement, camera, ultrasonic or emergency-stop test has been rerun on a device.
2. **Right-curving motion remains unresolved.** The 2023 manual test record contains one failed case for straight-line travel. Both original motor commands use `startMove(40,40)` and were intentionally preserved.
3. **Vendor calls can block.** Software sample/time limits apply between API returns, not to an indefinite blocking call within the SwiftBot dependency. Cleanup is best-effort rather than a verified emergency stop.
4. **Three tunnels are instructed, not enforced.** The operator is told to arrange at least three tunnels; stopping after fewer completed tunnel measurements remains possible.
5. **Measurement estimates are model-dependent.** Each segment length equals calibrated speed multiplied by elapsed segment time. Wheel slip, changing speed and camera transition latency can affect accuracy.
6. **Light-intensity thresholds are not physically recalibrated.** Changes in surface reflectivity, camera exposure, ambient brightness and tunnel geometry could affect detection.
7. **Partial segments are not reported as completed lengths.** Only finished tunnel and gap intervals contribute to totals.
8. **Historical example discrepancy.** The original report's sample log contains two tunnel lengths that differ by 32.4 cm, conflicting with the stated 10–15 cm consecutive-tunnel variation rule. That discrepancy has not been reclassified as a passing physical measurement.
9. **Environment-dependent deployment.** Compilation against the supplied SwiftBot JAR succeeded with Java 11-target bytecode; Raspberry Pi execution, an actual Java 11 runtime and Eclipse GUI import were not checked.
10. **Local proprietary dependency.** `SwiftBotAPI-4.0.2.jar` must be obtained legitimately and supplied locally. The repository does not redistribute vendor binaries or provide automatic CI builds.
