package io.github.nakib16k.tunnelvision;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/** No SwiftBot hardware is instantiated or operated by these tests. */
public final class CoreLogicTests {
    private static int checks;

    public static void main(String[] args) throws Exception {
        testPixelIntensities();
        testJourneyReport();
        testLocaleIndependentReporting();
        testEmptyAndPartialJourneys();
        testConsoleInput();
        testSegmentTiming();
        testTransitions();
        testDistanceRequirements();
        testSyntheticJourneyIntegration();
        System.out.println("PASS: " + checks + " offline assertions (9 test groups), 0 failures");
    }

    private static void testPixelIntensities() {
        BufferedImage black = filled(48, 48, 0);
        eq(0, ImageIntensityAnalyzer.calculateAverageIntensity(black), "black 48x48");
        eq(255, ImageIntensityAnalyzer.calculateAverageIntensity(filled(48, 48, 255)), "white 48x48");
        eq(127, ImageIntensityAnalyzer.calculateAverageIntensity(filled(48, 48, 127)), "gray 48x48");
        BufferedImage mixed = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        mixed.setRGB(0, 0, 0xff000000);
        mixed.setRGB(1, 0, 0xffffffff);
        mixed.setRGB(0, 1, 0xff143250); // 20 + 50 + 80 = 150
        mixed.setRGB(1, 1, 0xff781e00); // 120 + 30 + 0 = 150
        eq(89, ImageIntensityAnalyzer.calculateAverageIntensity(mixed), "RGB mean across pixels");
        BufferedImage two = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
        two.setRGB(0, 0, 0xff000000);
        two.setRGB(1, 0, 0xffffffff);
        eq(128, ImageIntensityAnalyzer.calculateAverageIntensity(two), "rounded pixel mean");
        throwsType(IllegalArgumentException.class,
                () -> ImageIntensityAnalyzer.calculateAverageIntensity(null), "reject null image");
    }

    private static void testJourneyReport() {
        String single = log(new double[]{45}, new double[]{5}, new double[]{}, new double[]{});
        has(single, "Length of Tunnel 1 = 45.0cm", "one tunnel logged");
        lacks(single, "is Maximum", "single tunnel omits maximum");
        lacks(single, "is Minimum", "single tunnel omits minimum");
        String rising = log(new double[]{30, 42, 51}, new double[]{3, 4, 5},
                new double[]{20, 22}, new double[]{2, 2});
        has(rising, "The Tunnel: 3 of Length = 51.0cm is Maximum", "rising maximum");
        has(rising, "The Tunnel: 1 of Length = 30.0cm is Minimum", "rising minimum");
        has(rising, "The Total Distance = 165.0cm", "all recorded lengths counted");
        has(rising, "The Total Duration = 16.0s", "all durations counted");
        String falling = log(new double[]{60, 48, 35}, new double[]{1, 1, 1},
                new double[]{}, new double[]{});
        has(falling, "The Tunnel: 1 of Length = 60.0cm is Maximum", "falling maximum");
        has(falling, "The Tunnel: 3 of Length = 35.0cm is Minimum", "falling minimum");
        String ties = log(new double[]{40, 40, 40}, new double[]{1, 1, 1},
                new double[]{}, new double[]{});
        has(ties, "The Tunnel: 1 of Length = 40.0cm is Maximum", "ties earliest maximum");
        has(ties, "The Tunnel: 1 of Length = 40.0cm is Minimum", "ties earliest minimum");
        String nonFirstMin = log(new double[]{90, 30, 70}, new double[]{1, 1, 1},
                new double[]{}, new double[]{});
        has(nonFirstMin, "The Tunnel: 2 of Length = 30.0cm is Minimum", "interior minimum");
        has(nonFirstMin, "The Tunnel: 1 of Length = 90.0cm is Maximum", "interior max comparator");
    }

    private static void testLocaleIndependentReporting() {
        Locale before = Locale.getDefault();
        try {
            Locale.setDefault(Locale.FRANCE);
            String report = log(new double[]{35.5}, new double[]{3.2},
                    new double[]{20.0}, new double[]{2.1});
            has(report, "Length of Tunnel 1 = 35.5cm", "locale-independent decimal mark");
            has(report, "The Total Duration = 5.3s", "locale-independent timing decimal");
        } finally {
            Locale.setDefault(before);
        }
    }

    private static void testEmptyAndPartialJourneys() {
        String empty = log(new double[]{}, new double[]{}, new double[]{}, new double[]{});
        has(empty, "No tunnel measurements recorded.", "empty tunnels");
        has(empty, "No gap measurements recorded.", "empty gaps");
        has(empty, "The Total Distance = 0.0cm", "empty distance");
        has(empty, "The Total Duration = 0.0s", "empty time");
        String partial = log(new double[]{35, 45}, new double[]{3, 4},
                new double[]{25}, new double[]{2});
        has(partial, "The Total Distance = 105.0cm", "two tunnels and one gap");
        String gapOnly = log(new double[]{}, new double[]{},
                new double[]{25}, new double[]{2});
        has(gapOnly, "The Total Distance = 25.0cm", "gap-only recorded run");
        throwsType(IllegalArgumentException.class,
                () -> JourneyReport.format(Arrays.asList(45.0), Collections.emptyList(),
                        Collections.emptyList(), Collections.emptyList(), "DATE"),
                "length/time mismatch rejected");
        throwsType(IllegalArgumentException.class,
                () -> JourneyReport.format(Arrays.asList(Double.NaN), Arrays.asList(1.0),
                        Collections.emptyList(), Collections.emptyList(), "DATE"),
                "invalid length rejected");
    }

    private static void testConsoleInput() throws Exception {
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        PrintStream print = new PrintStream(sink, true, StandardCharsets.UTF_8.name());
        ConsoleMessages messages = new ConsoleMessages(print);
        ConsoleInputHandler handler = new ConsoleInputHandler(new Scanner("Y\n3\ny\n"), messages);
        handler.awaitCalibrationConfirmation();
        String output = sink.toString(StandardCharsets.UTF_8.name());
        has(output, "Incorrect Input", "invalid uppercase rejected");
        has(output, "speed calibration", "calibration reprompt");
        ConsoleInputHandler start = new ConsoleInputHandler(new Scanner("N\ny\n"), messages);
        start.awaitStartConfirmation();
        has(sink.toString(StandardCharsets.UTF_8.name()), "start the task", "start reprompt");
        eq(true, new ConsoleInputHandler(new Scanner("y\n"), messages).askWhetherToWriteLog(), "log yes");
        eq(false, new ConsoleInputHandler(new Scanner("n\n"), messages).askWhetherToWriteLog(), "log no");
        eq(false, new ConsoleInputHandler(new Scanner("yes\nY\nn\n"), messages).askWhetherToWriteLog(), "invalid then no");
        has(sink.toString(StandardCharsets.UTF_8.name()), "Do you want a log?", "original log prompt wording preserved");
        lacks(sink.toString(StandardCharsets.UTF_8.name()), "writeAndDisplayJourneyLog", "method name not displayed");
        throwsType(IllegalStateException.class,
                () -> new ConsoleInputHandler(new Scanner(""), messages).awaitStartConfirmation(),
                "EOF does not loop forever");
        print.close();
    }

    private static void testSegmentTiming() {
        AtomicLong ns = new AtomicLong(0L);
        SegmentTimer timer = new SegmentTimer(ns::get);
        eq(false, timer.isRunning(), "timer initially stopped");
        timer.startSegmentTimer();
        eq(true, timer.isRunning(), "timer running");
        ns.set(2_500_000_000L);
        near(2.5, timer.stopSegmentTimer(), "elapsed 2.5s");
        eq(false, timer.isRunning(), "timer stopped");
        throwsType(IllegalStateException.class, timer::stopSegmentTimer, "double stop forbidden");
        ns.set(4_000_000_000L);
        timer.startSegmentTimer();
        throwsType(IllegalStateException.class, timer::startSegmentTimer, "double start forbidden");
        ns.set(5_000_000_000L);
        near(1.0, timer.stopSegmentTimer(), "timer restart resets origin");
        ns.set(7_000_000_000L);
        timer.startSegmentTimer();
        ns.set(6_000_000_000L);
        near(0.0, timer.stopSegmentTimer(), "reversed fake clock clamped");
    }

    private static void testTransitions() {
        TunnelStateDetector d = new TunnelStateDetector();
        eq(TunnelStateDetector.State.INSIDE_TUNNEL, d.getState(), "starts inside");
        eq(TunnelStateDetector.Transition.NONE, d.acceptIntensity(40), "first reading baseline");
        eq(TunnelStateDetector.Transition.NONE, d.acceptIntensity(45), "+5 not enough to trigger");
        eq(TunnelStateDetector.Transition.NONE, d.acceptIntensity(80), "exit change detected");
        eq(TunnelStateDetector.Transition.EXITED_TUNNEL, d.acceptIntensity(85), "exit after stabilisation");
        eq(TunnelStateDetector.State.OUTSIDE_TUNNEL, d.getState(), "outside after exit");
        eq(TunnelStateDetector.Transition.NONE, d.acceptIntensity(80), "-5 not enough to enter");
        eq(TunnelStateDetector.Transition.NONE, d.acceptIntensity(30), "entry change detected");
        eq(TunnelStateDetector.Transition.ENTERED_TUNNEL, d.acceptIntensity(28), "entry after stabilisation");
        eq(TunnelStateDetector.State.INSIDE_TUNNEL, d.getState(), "inside after entry");
        eq(TunnelStateDetector.Transition.NONE, d.acceptIntensity(27), "no duplicate entry");
        TunnelStateDetector noJump = new TunnelStateDetector();
        noJump.acceptIntensity(50);
        noJump.acceptIntensity(90);
        eq(TunnelStateDetector.Transition.NONE, noJump.acceptIntensity(60), "direction reversal cancels exit");
        eq(TunnelStateDetector.State.INSIDE_TUNNEL, noJump.getState(), "no impossible state jump");
        TunnelStateDetector boundaries = new TunnelStateDetector();
        boundaries.acceptIntensity(20);
        boundaries.acceptIntensity(50);
        eq(TunnelStateDetector.Transition.EXITED_TUNNEL, boundaries.acceptIntensity(70),
                "stabilisation +20 boundary");
        TunnelStateDetector delayed = new TunnelStateDetector();
        delayed.acceptIntensity(10);
        delayed.acceptIntensity(50);
        eq(TunnelStateDetector.Transition.NONE, delayed.acceptIntensity(80), "not stable at +30");
        eq(TunnelStateDetector.Transition.EXITED_TUNNEL, delayed.acceptIntensity(85),
                "delayed stabilisation");
        throwsType(IllegalArgumentException.class, () -> delayed.acceptIntensity(-1), "reject negative brightness");
        throwsType(IllegalArgumentException.class, () -> delayed.acceptIntensity(256), "reject high brightness");
    }

    private static void testDistanceRequirements() {
        near(30, DistanceChecks.calculateDistance(10, 3), "time times speed");
        near(0, DistanceChecks.calculateDistance(0, 0), "zero distance");
        eq(false, DistanceChecks.isValidTunnel(29.999, null), "tunnel below 30");
        eq(true, DistanceChecks.isValidTunnel(30, null), "tunnel exactly 30");
        eq(true, DistanceChecks.isValidTunnel(40, 30.0), "variation exactly 10");
        eq(true, DistanceChecks.isValidTunnel(45, 30.0), "variation exactly 15");
        eq(true, DistanceChecks.isValidTunnel(30, 40.0), "negative direction difference 10");
        eq(true, DistanceChecks.isValidTunnel(30, 45.0), "negative direction difference 15");
        eq(false, DistanceChecks.isValidTunnel(39.99, 30.0), "variation below 10");
        eq(false, DistanceChecks.isValidTunnel(45.01, 30.0), "variation above 15");
        eq(false, DistanceChecks.isValidTunnel(Double.NaN, null), "reject NaN tunnel");
        eq(false, DistanceChecks.isValidGap(19.99), "gap below 20");
        eq(true, DistanceChecks.isValidGap(20), "gap exactly 20");
        eq(false, DistanceChecks.isValidGap(Double.POSITIVE_INFINITY), "reject infinite gap");
        throwsType(IllegalArgumentException.class, () -> DistanceChecks.calculateDistance(-1, 2), "negative speed");
        throwsType(IllegalArgumentException.class, () -> DistanceChecks.calculateDistance(1, -2), "negative time");
        throwsType(IllegalArgumentException.class, () -> DistanceChecks.calculateDistance(Double.NaN, 2), "NaN speed");
    }

    /** Offline integration: transition -> timer -> distance -> log, no API calls. */
    private static void testSyntheticJourneyIntegration() {
        AtomicLong clock = new AtomicLong(0L);
        SegmentTimer timer = new SegmentTimer(clock::get);
        TunnelStateDetector detector = new TunnelStateDetector();
        java.util.List<Double> tunnels = new java.util.ArrayList<>();
        java.util.List<Double> tunnelDurations = new java.util.ArrayList<>();
        java.util.List<Double> gaps = new java.util.ArrayList<>();
        java.util.List<Double> gapDurations = new java.util.ArrayList<>();
        double speed = 10.0;
        timer.startSegmentTimer();
        eq(TunnelStateDetector.Transition.NONE, detector.acceptIntensity(40), "integration initial sample");
        clock.set(2_000_000_000L);
        eq(TunnelStateDetector.Transition.NONE, detector.acceptIntensity(90), "integration rising intensity");
        clock.set(3_000_000_000L);
        eq(TunnelStateDetector.Transition.EXITED_TUNNEL, detector.acceptIntensity(93), "integration exit");
        double duration = timer.stopSegmentTimer();
        tunnels.add(DistanceChecks.calculateDistance(speed, duration));
        tunnelDurations.add(duration);
        eq(true, DistanceChecks.isValidTunnel(tunnels.get(0), null), "integration first tunnel valid");
        timer.startSegmentTimer();
        clock.set(4_000_000_000L);
        eq(TunnelStateDetector.Transition.NONE, detector.acceptIntensity(30), "integration falling intensity");
        clock.set(5_000_000_000L);
        eq(TunnelStateDetector.Transition.ENTERED_TUNNEL, detector.acceptIntensity(31), "integration enter");
        duration = timer.stopSegmentTimer();
        gaps.add(DistanceChecks.calculateDistance(speed, duration));
        gapDurations.add(duration);
        eq(true, DistanceChecks.isValidGap(gaps.get(0)), "integration gap valid");
        timer.startSegmentTimer();
        clock.set(7_000_000_000L);
        eq(TunnelStateDetector.Transition.NONE, detector.acceptIntensity(80), "integration second rising");
        clock.set(9_000_000_000L);
        eq(TunnelStateDetector.Transition.EXITED_TUNNEL, detector.acceptIntensity(82), "integration second exit");
        duration = timer.stopSegmentTimer();
        tunnels.add(DistanceChecks.calculateDistance(speed, duration));
        tunnelDurations.add(duration);
        eq(true, DistanceChecks.isValidTunnel(tunnels.get(1), tunnels.get(0)), "integration valid 10 cm variation");
        String report = JourneyReport.format(tunnels, tunnelDurations, gaps, gapDurations, "SIMULATION");
        has(report, "Length of Tunnel 1 = 30.0cm", "integration first length");
        has(report, "Length of Tunnel 2 = 40.0cm", "integration second length");
        has(report, "The Total Distance = 90.0cm", "integration summed distance");
        has(report, "The Total Duration = 9.0s", "integration summed durations");
        has(report, "The Tunnel: 1 of Length = 30.0cm is Minimum", "integration extrema");
    }

    private static BufferedImage filled(int width, int height, int intensity) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int rgb = (intensity << 16) | (intensity << 8) | intensity;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) image.setRGB(x, y, rgb);
        }
        return image;
    }

    private static String log(double[] tunnels, double[] tunnelTimes, double[] gaps, double[] gapTimes) {
        return JourneyReport.format(toList(tunnels), toList(tunnelTimes), toList(gaps), toList(gapTimes), "DATE");
    }

    private static java.util.List<Double> toList(double[] values) {
        java.util.List<Double> list = new java.util.ArrayList<>();
        for (double value : values) list.add(value);
        return list;
    }

    private static void has(String text, String expected, String reason) {
        check(text.contains(expected), reason + " expected text: " + expected);
    }

    private static void lacks(String text, String unexpected, String reason) {
        check(!text.contains(unexpected), reason + " unexpectedly contained: " + unexpected);
    }

    private static void near(double expected, double actual, String reason) {
        check(Math.abs(expected - actual) < 1e-8, reason + " expected " + expected + ", got " + actual);
    }

    private static void eq(Object expected, Object actual, String reason) {
        check(expected.equals(actual), reason + " expected " + expected + ", got " + actual);
    }

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError("CHECK " + checks + " FAILED: " + reason);
    }

    private static void throwsType(Class<? extends Throwable> expected, Runnable operation, String reason) {
        checks++;
        try {
            operation.run();
        } catch (Throwable problem) {
            if (expected.isInstance(problem)) return;
            throw new AssertionError("CHECK " + checks + " FAILED: " + reason + " wrong exception " + problem);
        }
        throw new AssertionError("CHECK " + checks + " FAILED: " + reason + " expected " + expected.getSimpleName());
    }
}
