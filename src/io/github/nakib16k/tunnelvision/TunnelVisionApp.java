package io.github.nakib16k.tunnelvision;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.LongSupplier;

/**
 * Tunnel Vision originated as individual SwiftBot coursework in 2022–23.
 * This repository includes refinements made after evaluating that coursework;
 * those refinements have not been validated on physical SwiftBot hardware.
 */
public final class TunnelVisionApp {
    private final RobotPort robot;
    private final ConsoleMessages messages;
    private final ConsoleInputHandler input;
    private final AppConfig config;
    private final PrintStream out;
    private final PrintStream err;
    private final LongSupplier nanoClock;
    private final UltrasonicCalibration.Sleeper sleeper;
    private final SegmentTimer segmentTimer;
    private final TunnelStateDetector detector = new TunnelStateDetector();
    private final CameraFailurePolicy cameraFailures = new CameraFailurePolicy(AppConfig.MAX_CAMERA_FAILURES);
    private final ArrayList<Double> tunnelLengths = new ArrayList<>();
    private final ArrayList<Double> tunnelTimes = new ArrayList<>();
    private final ArrayList<Double> gapLengths = new ArrayList<>();
    private final ArrayList<Double> gapTimes = new ArrayList<>();
    private final DecimalFormat df = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.ROOT));
    private double speedCmPerSecond;

    public TunnelVisionApp(RobotPort robot, ConsoleInputHandler input, ConsoleMessages messages,
                           AppConfig config, PrintStream out, PrintStream err,
                           LongSupplier nanoClock, UltrasonicCalibration.Sleeper sleeper) {
        this.robot = Objects.requireNonNull(robot);
        this.input = Objects.requireNonNull(input);
        this.messages = Objects.requireNonNull(messages);
        this.config = Objects.requireNonNull(config);
        this.out = Objects.requireNonNull(out);
        this.err = Objects.requireNonNull(err);
        this.nanoClock = Objects.requireNonNull(nanoClock);
        this.sleeper = Objects.requireNonNull(sleeper);
        this.segmentTimer = new SegmentTimer(nanoClock);
    }

    public static void main(String[] args) {
        try {
            ConsoleMessages messages = new ConsoleMessages(System.out);
            ConsoleInputHandler input = new ConsoleInputHandler(new Scanner(System.in), messages);
            TunnelVisionApp app = new TunnelVisionApp(new SwiftBotRobotAdapter(), input, messages,
                    AppConfig.fromSystem(), System.out, System.err, System::nanoTime, Thread::sleep);
            if (!app.run()) System.err.println("Tunnel Vision ended with an error. See messages above.");
        } catch (Exception | LinkageError failure) {
            System.err.println("Tunnel Vision could not initialise the SwiftBot: " + failure);
        }
    }

    /** Run once. Devices are stopped and shut down on both success and failure. */
    public boolean run() {
        boolean journeyStarted = false;
        boolean completedWithoutError = false;
        boolean cleanupSuccessful;
        try {
            // Avoid starting motors when the selected directory cannot be prepared.
            Files.createDirectories(config.getDataDirectory());
            messages.showWelcomeMessage();
            input.awaitCalibrationConfirmation();
            speedCmPerSecond = UltrasonicCalibration.calibrate(robot, nanoClock,
                    AppConfig.MAX_CALIBRATION_SAMPLES, AppConfig.MAX_CALIBRATION_NANOS, sleeper);
            out.println("\nSpeed = " + df.format(speedCmPerSecond) + "cm/s");
            messages.showTunnelSetupInstructions();
            input.awaitStartConfirmation();
            messages.showRunStartedMessage();
            robot.startMove(40, 40); // Original 2023 steering, intentionally unchanged.
            journeyStarted = true;
            robot.setRedUnderlights();
            segmentTimer.startSegmentTimer();
            messages.showInsideTunnelStatus();
            completedWithoutError = runMeasurementLoop();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            err.println("ERROR: Operation interrupted; stopping SwiftBot.");
        } catch (IOException | RuntimeException error) {
            err.println("ERROR: Journey aborted safely: " + error.getMessage());
        } finally {
            if (segmentTimer.isRunning()) segmentTimer.stopSegmentTimer();
            cleanupSuccessful = RobotSafety.stopAndShutdown(robot, err);
        }
        // Shutdown occurs before waiting for the operator to answer the log prompt.
        // An aborted run may still contain useful, completed segment measurements.
        if (journeyStarted) {
            messages.showLogPrompt();
            try {
                if (input.askWhetherToWriteLog()) {
                    String report = JourneyReport.format(tunnelLengths, tunnelTimes,
                            gapLengths, gapTimes, new Date().toString());
                    boolean saved = JourneyLogWriter.appendAndDisplay(config.getLogFile(), report, out, err);
                    if (!saved) completedWithoutError = false;
                }
            } catch (RuntimeException logError) {
                err.println("ERROR: Unable to finish log request: " + logError.getMessage());
                completedWithoutError = false;
            }
        }
        return completedWithoutError && cleanupSuccessful;
    }

    private boolean runMeasurementLoop() throws IOException, InterruptedException {
        while (robot.isStopButtonReleased()) {
            sleeper.sleep(150L);
            int intensity;
            try {
                intensity = CameraFrameProcessor.captureIntensity(robot, config.getDataDirectory());
                cameraFailures.recordSuccess();
            } catch (IOException captureFailure) {
                detector.resetAfterLostFrame();
                err.println("WARNING: Camera capture failed (" + (cameraFailures.getConsecutiveFailures() + 1)
                        + "/" + AppConfig.MAX_CAMERA_FAILURES + "): " + captureFailure.getMessage());
                if (cameraFailures.recordFailure()) {
                    throw new IOException("Camera failed " + AppConfig.MAX_CAMERA_FAILURES
                            + " consecutive times; stopping the robot", captureFailure);
                }
                continue;
            }
            sleeper.sleep(150L);
            TunnelStateDetector.Transition transition = detector.acceptIntensity(intensity);
            if (transition == TunnelStateDetector.Transition.EXITED_TUNNEL) {
                double elapsed = segmentTimer.stopSegmentTimer();
                double length = DistanceChecks.calculateDistance(speedCmPerSecond, elapsed);
                tunnelTimes.add(elapsed);
                tunnelLengths.add(length);
                messages.showOutsideTunnelStatus();
                out.println("Length of Tunnel " + tunnelLengths.size() + " = " + df.format(length) + "cm");
                if (!validateLatestTunnelLength()) return false;
                messages.showStopInstructions();
                segmentTimer.startSegmentTimer();
            } else if (transition == TunnelStateDetector.Transition.ENTERED_TUNNEL) {
                double elapsed = segmentTimer.stopSegmentTimer();
                double length = DistanceChecks.calculateDistance(speedCmPerSecond, elapsed);
                gapTimes.add(elapsed);
                gapLengths.add(length);
                messages.showInsideTunnelStatus();
                out.println("The Distance of Gap number: " + gapLengths.size()
                        + " between two consecutive tunnels = " + df.format(length) + "cm");
                if (!validateLatestGapLength()) return false;
                segmentTimer.startSegmentTimer();
            }
        }
        return true;
    }

    private boolean validateLatestTunnelLength() {
        int latest = tunnelLengths.size() - 1;
        double length = tunnelLengths.get(latest);
        Double previous = latest > 0 ? tunnelLengths.get(latest - 1) : null;
        if (!DistanceChecks.isValidTunnel(length, previous)) {
            if (length < DistanceChecks.MIN_TUNNEL_CM) {
                messages.showMinimumTunnelLengthError();
            } else {
                messages.showTunnelVariationError();
            }
            return false;
        }
        return true;
    }

    private boolean validateLatestGapLength() {
        if (!DistanceChecks.isValidGap(gapLengths.get(gapLengths.size() - 1))) {
            out.println("There isn't sufficient gap in the Gap Number: " + gapLengths.size());
            return false;
        }
        return true;
    }
}
