package io.github.nakib16k.tunnelvision;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/** Offline checks: all robot operations are simulated, never run on a physical SwiftBot. */
public final class ReliabilityTests {
    private static int assertions;

    public static void main(String[] args) throws Exception {
        testConfigAndPaths();
        testLogAppendAndErrorReporting();
        testCameraNullFailureAndNoStaleImage();
        testInvalidUltrasonicDistances();
        testCalibrationSuccessAndBoundedFailure();
        testCleanupAcrossFailures();
        testRunAbortAfterThreeFrames();
        testRunSuccessfulStopAndLog();
        testInvalidTunnelDoesNotReportSuccess();
        System.out.println("PASS: " + assertions + " reliability offline assertions (9 groups), 0 failures");
    }

    private static void testConfigAndPaths() throws Exception {
        Path temp = Files.createTempDirectory("tv-c-path-");
        AppConfig config = new AppConfig(temp.resolve("sub/.."));
        eq(temp.toAbsolutePath().normalize(), config.getDataDirectory(), "configured path normalized");
        eq(temp.resolve("LOG.txt"), config.getLogFile(), "log inside data directory");
        String original = System.getProperty("tunnelvision.dataDir");
        try {
            System.setProperty("tunnelvision.dataDir", temp.resolve("custom").toString());
            eq(temp.resolve("custom").toAbsolutePath(), AppConfig.fromSystem().getDataDirectory(),
                    "system property config override");
        } finally {
            if (original == null) System.clearProperty("tunnelvision.dataDir");
            else System.setProperty("tunnelvision.dataDir", original);
        }
        throwsType(NullPointerException.class, () -> new AppConfig(null), "null directory rejected");
    }

    private static void testLogAppendAndErrorReporting() throws Exception {
        Path temp = Files.createTempDirectory("tv-c-log-");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(output, true, "UTF-8");
        PrintStream err = new PrintStream(errors, true, "UTF-8");
        Path reportFile = temp.resolve("nested").resolve("LOG.txt");
        check(JourneyLogWriter.appendAndDisplay(reportFile, "report-A\n", out, err), "first log persisted");
        check(JourneyLogWriter.appendAndDisplay(reportFile, "report-B\n", out, err), "second log appended");
        eq("report-A\n" + System.lineSeparator() + "report-B\n" + System.lineSeparator(),
                new String(Files.readAllBytes(reportFile), StandardCharsets.UTF_8), "two logs appended without overwrite");
        check(output.toString("UTF-8").contains("report-A") && output.toString("UTF-8").contains("report-B"),
                "both reports displayed");
        Path blockingFile = temp.resolve("not-a-directory");
        Files.write(blockingFile, Arrays.asList("x"), StandardCharsets.UTF_8);
        check(!JourneyLogWriter.appendAndDisplay(blockingFile.resolve("LOG.txt"), "REPORT RECOVERY", out, err),
                "unwritable path returns failed status");
        check(errors.toString("UTF-8").contains("ERROR: Unable to save journey log"), "write error explicit");
        check(output.toString("UTF-8").contains("REPORT RECOVERY"), "report displayed after write error");
    }

    private static void testCameraNullFailureAndNoStaleImage() throws Exception {
        SpyRobot robot = new SpyRobot();
        Path temp = Files.createTempDirectory("tv-c-frame-");
        robot.captured = null;
        throwsType(IOException.class, () -> uncheckedFrame(robot, temp), "null frame rejected");
        BufferedImage valid = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        valid.setRGB(0, 0, 0x00ffffff);
        robot.captured = valid;
        eq(64, CameraFrameProcessor.captureIntensity(robot, temp), "real fresh image mean (round 63.75)");
        robot.captured = null;
        throwsType(IOException.class, () -> uncheckedFrame(robot, temp), "stale frame not reused on next failure");
        robot.failCapture = true;
        throwsType(IOException.class, () -> uncheckedFrame(robot, temp), "capture exception rejected");
        eq(temp, robot.lastImageDir, "configured capture path forwarded to camera");
        CameraFailurePolicy policy = new CameraFailurePolicy(3);
        check(!policy.recordFailure(), "one camera miss tolerable");
        check(!policy.recordFailure(), "two camera misses tolerable");
        policy.recordSuccess();
        eq(0, policy.getConsecutiveFailures(), "success resets camera counter");
        check(!policy.recordFailure(), "first miss after success tolerable");
        check(!policy.recordFailure(), "second miss after success tolerable");
        check(policy.recordFailure(), "third consecutive miss stops");
        throwsType(IllegalArgumentException.class, () -> new CameraFailurePolicy(0), "zero failure threshold rejected");
        TunnelStateDetector detector = new TunnelStateDetector();
        eq(TunnelStateDetector.Transition.NONE, detector.acceptIntensity(10), "initial baseline");
        eq(TunnelStateDetector.Transition.NONE, detector.acceptIntensity(40), "pending exit detection");
        detector.resetAfterLostFrame();
        eq(TunnelStateDetector.State.INSIDE_TUNNEL, detector.getState(), "physical state preserved after loss");
        eq(TunnelStateDetector.Transition.NONE, detector.acceptIntensity(40), "new baseline after lost frame");
        eq(TunnelStateDetector.Transition.NONE, detector.acceptIntensity(40), "old pending transition not reused");
    }

    private static void uncheckedFrame(SpyRobot robot, Path dir) {
        try { CameraFrameProcessor.captureIntensity(robot, dir); }
        catch (IOException | InterruptedException error) { throw new WrappedCheckedException(error); }
    }

    private static void testInvalidUltrasonicDistances() {
        for (double value : new double[] {0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY}) {
            throwsType(IllegalArgumentException.class, () -> UltrasonicCalibration.checkedDistance(value),
                    "invalid ultrasonic value rejected: " + value);
        }
        near(16.0, UltrasonicCalibration.checkedDistance(16.0), "valid reading accepted");
        for (double value : new double[] {0.0, -2.0, Double.NaN, Double.POSITIVE_INFINITY, 15.0}) {
            SpyRobot robot = new SpyRobot();
            robot.readings.add(value);
            throwsType(RuntimeException.class, () -> calibrateUnchecked(robot, () -> 0L, 10, 20_000_000_000L),
                    "invalid start distance aborts");
            eq(0, robot.starts, "no motor movement for invalid initial reading");
        }
    }

    private static void testCalibrationSuccessAndBoundedFailure() {
        SpyRobot robot = new SpyRobot();
        robot.readings.add(50.0);
        robot.readings.add(40.0);
        robot.readings.add(14.0);
        AtomicLong clock = new AtomicLong();
        double speed = calibrateUnchecked(robot, () -> clock.getAndAdd(1_000_000_000L), 10, 20_000_000_000L);
        near(12.0, speed, "positive calibrated speed from distance and elapsed time");
        eq(1, robot.starts, "calibration starts wheels once");
        eq(1, robot.stops, "calibration always stops wheels");
        eq("40,40", robot.lastSteering, "2023 steering arguments unchanged");

        SpyRobot stuck = new SpyRobot();
        stuck.readings.add(50.0);
        stuck.defaultReading = 50.0;
        throwsType(IllegalStateException.class, () -> calibrateUnchecked(stuck, () -> 0L, 3, 20_000_000_000L),
                "stuck sensor ends after bounded attempts even if clock does not advance");
        eq(1, stuck.stops, "stuck calibration stops motors");

        SpyRobot timedOut = new SpyRobot();
        timedOut.readings.add(50.0);
        AtomicLong timeoutClock = new AtomicLong();
        throwsType(IllegalStateException.class,
                () -> calibrateUnchecked(timedOut, () -> timeoutClock.getAndAdd(21_000_000_000L), 99, 20_000_000_000L),
                "calibration timer limit enforced");
        eq(1, timedOut.stops, "timeout stops motors");

        SpyRobot badMidReading = new SpyRobot();
        badMidReading.readings.add(50.0);
        badMidReading.readings.add(Double.NaN);
        throwsType(IllegalArgumentException.class,
                () -> calibrateUnchecked(badMidReading, () -> 0L, 5, 20_000_000_000L),
                "invalid mid-run sensor value aborts");
        eq(1, badMidReading.stops, "invalid mid-run sensor stops motors");

        SpyRobot zeroDuration = new SpyRobot();
        zeroDuration.readings.add(50.0);
        zeroDuration.readings.add(10.0);
        throwsType(IllegalStateException.class,
                () -> calibrateUnchecked(zeroDuration, () -> 0L, 5, 20_000_000_000L),
                "zero elapsed calibration duration rejected");
        eq(1, zeroDuration.stops, "invalid duration stops motors");
    }

    private static double calibrateUnchecked(SpyRobot robot, LongSupplier clock, int samples, long timeout) {
        try { return UltrasonicCalibration.calibrate(robot, clock, samples, timeout, millis -> { }); }
        catch (InterruptedException e) { throw new AssertionError(e); }
    }

    private static void testCleanupAcrossFailures() throws Exception {
        SpyRobot robot = new SpyRobot();
        robot.failStop = true;
        robot.failDisable = true;
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        check(!RobotSafety.stopAndShutdown(robot, new PrintStream(error, true, "UTF-8")),
                "cleanup reports failure rather than assuming success");
        eq(1, robot.stops, "stop attempted");
        eq(1, robot.ledDisableCalls, "LED disable still attempted after stop failure");
        eq(1, robot.shutdownCalls, "shutdown still attempted after LED failure");
        check(error.toString("UTF-8").contains("ERROR stopping motors"), "motor failure surfaced");
        check(error.toString("UTF-8").contains("ERROR disabling LEDs"), "LED failure surfaced");
        robot.failStop = false;
        robot.failDisable = false;
        check(RobotSafety.stopAndShutdown(robot, System.err), "clean cleanup reports success");
    }

    private static void testRunAbortAfterThreeFrames() throws Exception {
        SpyRobot robot = new SpyRobot();
        robot.readings.add(50.0);
        robot.readings.add(10.0);
        robot.stopReleased = true;
        robot.captured = null;
        ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outBuf, true, "UTF-8");
        PrintStream err = new PrintStream(errBuf, true, "UTF-8");
        ConsoleMessages messages = new ConsoleMessages(out);
        ConsoleInputHandler input = new ConsoleInputHandler(new Scanner("y\ny\nn\n"), messages);
        AtomicLong clock = new AtomicLong(0L);
        Path temporary = Files.createTempDirectory("tv-c-run-error-");
        TunnelVisionApp app = new TunnelVisionApp(robot, input, messages, new AppConfig(temporary), out, err,
                () -> clock.getAndAdd(1_000_000_000L), millis -> { });
        check(!app.run(), "camera failures mark run unsuccessful");
        eq(3, robot.captures, "run stops after third camera failure");
        eq(2, robot.starts, "both calibration and journey use original motor start");
        eq("40,40", robot.lastSteering, "no motor balancing applied");
        check(robot.stops >= 2, "calibration and cleanup both stop motors");
        eq(1, robot.ledDisableCalls, "LEDs disabled when camera fails");
        eq(1, robot.shutdownCalls, "robot shutdown when camera fails");
        check(errBuf.toString("UTF-8").contains("3 consecutive times"), "camera abort explained");
    }

    private static void testRunSuccessfulStopAndLog() throws Exception {
        SpyRobot robot = new SpyRobot();
        robot.readings.add(50.0);
        robot.readings.add(10.0);
        robot.stopReleased = false; // Simulated X pressed after task start.
        Path directory = Files.createTempDirectory("tv-c-run-stop-");
        ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outBuf, true, "UTF-8");
        ConsoleMessages messages = new ConsoleMessages(out);
        ConsoleInputHandler input = new ConsoleInputHandler(new Scanner("y\ny\ny\n"), messages);
        AtomicLong clock = new AtomicLong(0L);
        TunnelVisionApp app = new TunnelVisionApp(robot, input, messages, new AppConfig(directory), out,
                System.err, () -> clock.getAndAdd(1_000_000_000L), millis -> { });
        check(app.run(), "simulated stop followed by log succeeds");
        eq(0, robot.captures, "no frames captured when X already pressed");
        eq(1, robot.shutdownCalls, "robot shutdown on normal stop");
        check(Files.exists(directory.resolve("LOG.txt")), "configured log path receives report");
        check(Files.readString(directory.resolve("LOG.txt")).contains("No tunnel measurements recorded."),
                "empty-journey report safely persisted");
        check(outBuf.toString("UTF-8").contains("Do you want a log?"), "original log prompt preserved");
    }

    private static void testInvalidTunnelDoesNotReportSuccess() throws Exception {
        SpyRobot robot = new SpyRobot();
        robot.readings.add(50.0);
        robot.readings.add(10.0);
        robot.stopReleased = true;
        robot.buttonChecksUntilPress = 3;
        robot.frames.add(grayscale(0));
        robot.frames.add(grayscale(40));
        robot.frames.add(grayscale(40));
        Path temp = Files.createTempDirectory("tv-c-invalid-tunnel-");
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buf, true, "UTF-8");
        ConsoleMessages messages = new ConsoleMessages(out);
        ConsoleInputHandler input = new ConsoleInputHandler(new Scanner("y\ny\nn\n"), messages);
        AtomicLong clock = new AtomicLong(0L);
        TunnelVisionApp app = new TunnelVisionApp(robot, input, messages, new AppConfig(temp), out,
                System.err, () -> clock.getAndAdd(1_000_000_000L), millis -> { });
        check(!app.run(), "invalid 20cm tunnel measurement fails run status");
        check(buf.toString("UTF-8").contains("needs to be atleast 30 cm"), "minimum-length error shown");
        eq(3, robot.captures, "three synthetic frames produced the tunnel exit");
        eq(1, robot.shutdownCalls, "shutdown still runs on invalid segment");
    }

    private static BufferedImage grayscale(int intensity) {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        int rgb = (intensity << 16) | (intensity << 8) | intensity;
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) image.setRGB(x, y, rgb);
        }
        return image;
    }

    private static void near(double expected, double actual, String name) {
        check(Math.abs(expected - actual) < 1e-9, name + " expected=" + expected + " actual=" + actual);
    }
    private static void eq(Object expected, Object actual, String name) {
        check(expected.equals(actual), name + " expected=" + expected + " actual=" + actual);
    }
    private static void check(boolean valid, String name) {
        assertions++;
        if (!valid) throw new AssertionError("FAIL: " + name);
    }
    private static void throwsType(Class<? extends Throwable> expected, Runnable action, String name) {
        try {
            action.run();
        } catch (Throwable actual) {
            if (actual instanceof WrappedCheckedException && actual.getCause() != null) {
                actual = actual.getCause();
            }
            if (expected.isInstance(actual)) { assertions++; return; }
            throw new AssertionError("FAIL: " + name + " wrong exception: " + actual, actual);
        }
        throw new AssertionError("FAIL: " + name + " did not throw " + expected.getSimpleName());
    }
    private static final class WrappedCheckedException extends RuntimeException {
        WrappedCheckedException(Exception cause) { super(cause); }
    }
    private static final class SpyRobot implements RobotPort {
        final Deque<Double> readings = new ArrayDeque<>();
        double defaultReading = 10.0;
        int starts, stops, captures, ledDisableCalls, shutdownCalls;
        String lastSteering;
        boolean stopReleased, failCapture, failStop, failDisable;
        int buttonChecksUntilPress = -1;
        final Deque<BufferedImage> frames = new ArrayDeque<>();
        Path lastImageDir;
        BufferedImage captured;

        @Override public double readUltrasonicDistanceCm() {
            return readings.isEmpty() ? defaultReading : readings.removeFirst();
        }
        @Override public void startMove(int left, int right) {
            starts++;
            lastSteering = left + "," + right;
        }
        @Override public void stopMove() {
            stops++;
            if (failStop) throw new IllegalStateException("mock motor failure");
        }
        @Override public boolean isStopButtonReleased() {
            if (buttonChecksUntilPress >= 0) return buttonChecksUntilPress-- > 0;
            return stopReleased;
        }
        @Override public void setRedUnderlights() { }
        @Override public BufferedImage captureGrayscale(Path directory) throws IOException {
            captures++;
            lastImageDir = directory;
            if (failCapture) throw new IOException("corrupt or unreadable mock frame");
            return frames.isEmpty() ? captured : frames.removeFirst();
        }
        @Override public void disableUnderlights() {
            ledDisableCalls++;
            if (failDisable) throw new IllegalStateException("mock LED failure");
        }
        @Override public void shutdown() { shutdownCalls++; }
    }
}
