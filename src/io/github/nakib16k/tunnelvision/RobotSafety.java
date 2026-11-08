package io.github.nakib16k.tunnelvision;

import java.io.PrintStream;
import java.util.Objects;

/** Best-effort cleanup: one failing operation must not prevent later cleanup calls. */
public final class RobotSafety {
    private RobotSafety() { }

    public static boolean stopAndShutdown(RobotPort robot, PrintStream err) {
        Objects.requireNonNull(robot);
        Objects.requireNonNull(err);
        boolean success = true;
        try { robot.stopMove(); }
        catch (RuntimeException ex) { err.println("ERROR stopping motors: " + ex); success = false; }
        try { robot.disableUnderlights(); }
        catch (RuntimeException ex) { err.println("ERROR disabling LEDs: " + ex); success = false; }
        try { robot.shutdown(); }
        catch (RuntimeException ex) { err.println("ERROR shutting down robot: " + ex); success = false; }
        return success;
    }
}
