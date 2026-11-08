package io.github.nakib16k.tunnelvision;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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

/**
 * Independent negative-path and integration probes. No SwiftBot device is
 * created. These assertions do not establish hardware operation or safety.
 */
public final class IndependentRegressionTests {
    private static int count;
    private static int groups;
    private IndependentRegressionTests() { }

    public static void main(String[] args) throws Exception {
        testBlockedOutputDirectoryBeforeMotion();
        testInvalidStartingSensorReading();
        testInvalidGapAndStopAfterFailure();
        testFailedLogWriteMakesWholeRunFail();
        testShutdownFailureChangesReturnStatus();
        testInterruptedCalibrationCleansUp();
        testPromptAndLogContent();
        System.out.println("PASS: " + count + " independent regression offline assertions (" + groups
                + " groups), 0 failures");
    }

    private static void testBlockedOutputDirectoryBeforeMotion() throws Exception {
        groups++;
        Path base = Files.createTempDirectory("tv-d-blocked-");
        Path fileNotDir = base.resolve("already-a-file");
        Files.write(fileNotDir, Arrays.asList("blocked"), StandardCharsets.UTF_8);
        FakeRobot bot = new FakeRobot();
        Output output = new Output();
        check(!app(bot, output, fileNotDir, "", () -> 0L, millis -> { }).run(),
                "existing file cannot be output directory");
        eq(0, bot.starts, "directory preparation occurs before motor movement");
        eq(1, bot.shutdowns, "cleanup still shuts down robot on configuration failure");
        contains(output.errors(), "Journey aborted", "output-path failure explained");
    }

    private static void testInvalidStartingSensorReading() throws Exception {
        groups++;
        FakeRobot bot = new FakeRobot();
        bot.distances.add(15.0);
        Output output = new Output();
        check(!app(bot, output, Files.createTempDirectory("tv-d-sensor-"), "y\n",
                () -> 0L, millis -> { }).run(), "too-close starting reading fails run");
        eq(0, bot.starts, "no motor start before valid calibration input");
        eq(1, bot.stops, "cleanup still attempts motor stop after invalid sensor input");
        eq(1, bot.shutdowns, "cleanup still attempts shutdown after invalid sensor input");
    }

    private static void testInvalidGapAndStopAfterFailure() throws Exception {
        groups++;
        FakeRobot bot = new FakeRobot();
        bot.distances.add(50.0);
        bot.distances.add(10.0);
        bot.frames.add(frame(10));
        bot.frames.add(frame(40));
        bot.frames.add(frame(40));
        bot.frames.add(frame(10));
        bot.frames.add(frame(10));
        bot.frameBudget = 5;
        Deque<Long> timestamps = new ArrayDeque<>(Arrays.asList(
                0L, 1_000_000_000L, 2_000_000_000L,
                3_000_000_000L, 5_000_000_000L,
                6_000_000_000L, 6_500_000_000L));
        LongSupplier clock = () -> {
            if (timestamps.isEmpty()) throw new AssertionError("Unexpected extra time read");
            return timestamps.removeFirst();
        };
        Output output = new Output();
        check(!app(bot, output, Files.createTempDirectory("tv-d-gap-"), "y\ny\nn\n",
                clock, millis -> { }).run(), "10cm gap fails the minimum 20cm rule");
        eq(5, bot.captures, "synthetic stream reaches a valid tunnel then invalid gap");
        contains(output.normal(), "Length of Tunnel 1 = 40.0cm", "first tunnel passes 30cm minimum");
        contains(output.normal(), "There isn't sufficient gap in the Gap Number: 1", "gap rejection is explicit");
        eq(2, bot.stops, "calibration and final cleanup both attempt motor stop");
        eq(1, bot.shutdowns, "cleanup executes after invalid gap");
        eq(0, timestamps.size(), "expected timer calls consumed");
    }

    private static void testFailedLogWriteMakesWholeRunFail() throws Exception {
        groups++;
        FakeRobot bot = readyToStart();
        bot.frameBudget = 0; // operator presses stop without entering the loop
        Path dir = Files.createTempDirectory("tv-d-unwritable-log-");
        Files.createDirectory(dir.resolve("LOG.txt")); // cannot append a file to a directory
        Output output = new Output();
        check(!app(bot, output, dir, "y\ny\ny\n", tickingClock(), millis -> { }).run(),
                "log persistence failure marks run unsuccessful");
        contains(output.errors(), "Unable to save journey log", "failed file write disclosed");
        contains(output.normal(), "No tunnel measurements recorded.", "report still printed after write failure");
        eq(1, bot.shutdowns, "cleanup completed before logging error");
    }

    private static void testShutdownFailureChangesReturnStatus() throws Exception {
        groups++;
        FakeRobot bot = readyToStart();
        bot.frameBudget = 0;
        bot.failShutdown = true;
        Output output = new Output();
        check(!app(bot, output, Files.createTempDirectory("tv-d-shutdown-"), "y\ny\nn\n",
                tickingClock(), millis -> { }).run(), "shutdown error changes successful journey to failure");
        eq(1, bot.shutdowns, "shutdown was attempted once");
        eq(1, bot.ledDisables, "LED cleanup attempted independently");
        contains(output.errors(), "ERROR shutting down robot", "shutdown failure surfaced");
    }

    private static void testInterruptedCalibrationCleansUp() throws Exception {
        groups++;
        FakeRobot bot = new FakeRobot();
        bot.distances.add(50.0);
        bot.distances.add(50.0); // first polling call does not reach target -> sleeper invoked
        Output output = new Output();
        try {
            check(!app(bot, output, Files.createTempDirectory("tv-d-interrupted-"), "y\n",
                    tickingClock(), millis -> { throw new InterruptedException("simulated interrupt"); }).run(),
                    "interrupted calibration is a failed run");
            check(Thread.currentThread().isInterrupted(), "interrupt flag restored");
            eq(1, bot.starts, "motor was started for calibration");
            eq(2, bot.stops, "calibration finally and app cleanup both stop motors");
            eq(1, bot.shutdowns, "robot shut down after interruption");
            contains(output.errors(), "Operation interrupted", "interrupt explained");
        } finally {
            Thread.interrupted(); // avoid leaking flag to other tests
        }
    }

    private static void testPromptAndLogContent() throws Exception {
        groups++;
        FakeRobot bot = readyToStart();
        bot.frameBudget = 0;
        Path dir = Files.createTempDirectory("tv-d-complete-");
        Output output = new Output();
        check(app(bot, output, dir, "y\ny\ny\n", tickingClock(), millis -> { }).run(),
                "fully mocked instant-stop run succeeds");
        contains(output.normal(), "Tunnel Vision - SwiftBot Tunnel & Gap Measurement", "professional title retained");
        contains(output.normal(), "Do you want a log?", "original log prompt retained");
        check(!output.normal().contains("writeAndDisplayJourneyLog?"), "invalid log prompt wording absent");
        check(Files.exists(dir.resolve("LOG.txt")), "log persisted to configured directory");
        contains(Files.readString(dir.resolve("LOG.txt")), "The Total Distance = 0.0cm",
                "empty journey total is valid");
        eq(2, bot.starts, "both calibration and measurement retain original motor start calls");
        eq("40,40", bot.steering, "steering inputs never balanced");
    }

    private static FakeRobot readyToStart() {
        FakeRobot bot = new FakeRobot();
        bot.distances.add(50.0);
        bot.distances.add(10.0);
        return bot;
    }

    private static LongSupplier tickingClock() {
        AtomicLong ticks = new AtomicLong();
        return () -> ticks.getAndAdd(1_000_000_000L);
    }

    private static TunnelVisionApp app(FakeRobot bot, Output out, Path directory, String answers,
                                      LongSupplier clock, UltrasonicCalibration.Sleeper pause) {
        ConsoleMessages messages = new ConsoleMessages(out.out);
        ConsoleInputHandler input = new ConsoleInputHandler(new Scanner(answers), messages);
        return new TunnelVisionApp(bot, input, messages, new AppConfig(directory),
                out.out, out.err, clock, pause);
    }

    private static BufferedImage frame(int intensity) {
        BufferedImage result = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        int rgb = (intensity << 16) | (intensity << 8) | intensity;
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) result.setRGB(x, y, rgb);
        return result;
    }

    private static void check(boolean value, String description) {
        count++;
        if (!value) throw new AssertionError("D-" + count + ": " + description);
    }

    private static void eq(Object expected, Object actual, String description) {
        check(expected.equals(actual), description + ": expected " + expected + ", got " + actual);
    }

    private static void contains(String haystack, String needle, String description) {
        check(haystack.contains(needle), description + ": missing " + needle);
    }

    private static final class Output {
        final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(stdout, true, StandardCharsets.UTF_8);
        final PrintStream err = new PrintStream(stderr, true, StandardCharsets.UTF_8);
        String normal() { return stdout.toString(StandardCharsets.UTF_8); }
        String errors() { return stderr.toString(StandardCharsets.UTF_8); }
    }

    private static final class FakeRobot implements RobotPort {
        final Deque<Double> distances = new ArrayDeque<>();
        final Deque<BufferedImage> frames = new ArrayDeque<>();
        int frameBudget, starts, stops, shutdowns, ledDisables, captures;
        boolean failShutdown;
        String steering;
        @Override public double readUltrasonicDistanceCm() {
            return distances.isEmpty() ? 10.0 : distances.removeFirst();
        }
        @Override public void startMove(int left, int right) {
            starts++;
            steering = left + "," + right;
        }
        @Override public void stopMove() { stops++; }
        @Override public boolean isStopButtonReleased() { return captures < frameBudget; }
        @Override public void setRedUnderlights() { }
        @Override public BufferedImage captureGrayscale(Path directory) {
            captures++;
            return frames.isEmpty() ? frame(10) : frames.removeFirst();
        }
        @Override public void disableUnderlights() { ledDisables++; }
        @Override public void shutdown() {
            shutdowns++;
            if (failShutdown) throw new IllegalStateException("simulated shutdown failure");
        }
    }
}
