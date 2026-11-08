# SwiftBot API — local build dependency

The project requires a **legitimately obtained copy** of `SwiftBotAPI-4.0.2.jar` placed at `lib/SwiftBotAPI-4.0.2.jar` before running `ant compile` or `ant test`.

The vendor library is **not** committed or bundled, because redistribution permission has not been established. The original coursework archive used the same API version. No automated download, Raspberry Pi connection or remote deployment is performed by the build/tests.

The Java source targets Java 11 bytecode. Tests exercise a simulated robot interface rather than the original physical hardware.
