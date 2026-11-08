package io.github.nakib16k.tunnelvision;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/** Configurable output location and conservative software-only failure limits. */
public final class AppConfig {
    public static final int MAX_CAMERA_FAILURES = 3;
    public static final int MAX_CALIBRATION_SAMPLES = 250;
    public static final long MAX_CALIBRATION_NANOS = 20_000_000_000L;
    public static final long CALIBRATION_PAUSE_MILLIS = 20L;

    private final Path dataDirectory;

    public AppConfig(Path dataDirectory) {
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory").toAbsolutePath().normalize();
    }

    public static AppConfig fromSystem() {
        String chosen = System.getProperty("tunnelvision.dataDir");
        if (chosen == null || chosen.trim().isEmpty()) {
            chosen = System.getenv("TUNNEL_VISION_DATA_DIR");
        }
        if (chosen == null || chosen.trim().isEmpty()) {
            chosen = "/home/pi/Documents"; // Historical Raspberry Pi default; not hard-coded in business logic.
        }
        return new AppConfig(Paths.get(chosen));
    }

    public Path getDataDirectory() { return dataDirectory; }
    public Path getLogFile() { return dataDirectory.resolve("LOG.txt"); }
}
