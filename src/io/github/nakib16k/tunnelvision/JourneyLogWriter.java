package io.github.nakib16k.tunnelvision;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/** Append one complete report; always display it even if file persistence fails. */
public final class JourneyLogWriter {
    private JourneyLogWriter() { }

    public static boolean appendAndDisplay(Path logFile, String report,
                                            PrintStream out, PrintStream err) {
        Objects.requireNonNull(logFile);
        Objects.requireNonNull(report);
        Objects.requireNonNull(out);
        Objects.requireNonNull(err);
        boolean saved = false;
        try {
            Path parent = logFile.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.write(logFile, (report + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            saved = true;
        } catch (IOException | SecurityException e) {
            err.println("ERROR: Unable to save journey log at " + logFile + ": " + e.getMessage());
            err.println("The report is printed below so it is not silently discarded.");
        }
        out.print(report);
        return saved;
    }
}
