package io.github.nakib16k.tunnelvision;

import java.util.Objects;
import java.util.function.LongSupplier;

/** Bounded, fail-safe ultrasonic calibration using injected robot and clock. */
public final class UltrasonicCalibration {
    public interface Sleeper { void sleep(long millis) throws InterruptedException; }
    public static final double STOP_DISTANCE_CM = 15.0;

    private UltrasonicCalibration() { }

    public static double calibrate(RobotPort robot, LongSupplier clock,
                                   int maxSamples, long timeoutNanos,
                                   Sleeper pause) throws InterruptedException {
        Objects.requireNonNull(robot, "robot");
        Objects.requireNonNull(clock, "clock");
        Objects.requireNonNull(pause, "pause");
        if (maxSamples < 1 || timeoutNanos <= 0L) {
            throw new IllegalArgumentException("Calibration limits must be positive");
        }
        // Read and validate the starting distance BEFORE starting the wheels.
        double initialCm = checkedDistance(robot.readUltrasonicDistanceCm());
        if (initialCm <= STOP_DISTANCE_CM) {
            throw new IllegalStateException("Start more than 15 cm from the target");
        }
        long startedAt = clock.getAsLong();
        robot.startMove(40, 40); // Original 2023 motor parameters, deliberately unchanged.
        try {
            for (int sample = 0; sample < maxSamples; sample++) {
                if (clock.getAsLong() - startedAt >= timeoutNanos) {
                    throw new IllegalStateException("Ultrasonic calibration timed out");
                }
                double distanceCm = checkedDistance(robot.readUltrasonicDistanceCm());
                if (distanceCm < STOP_DISTANCE_CM) {
                    double seconds = (clock.getAsLong() - startedAt) / 1_000_000_000.0;
                    if (!Double.isFinite(seconds) || seconds <= 0.0) {
                        throw new IllegalStateException("Calibration duration must be positive");
                    }
                    double speed = (initialCm - distanceCm) / seconds;
                    if (!Double.isFinite(speed) || speed <= 0.0) {
                        throw new IllegalStateException("Calculated speed is invalid");
                    }
                    return speed;
                }
                pause.sleep(AppConfig.CALIBRATION_PAUSE_MILLIS);
            }
            throw new IllegalStateException("Ultrasonic calibration exceeded " + maxSamples + " readings");
        } finally {
            robot.stopMove();
        }
    }

    public static double checkedDistance(double centimetres) {
        if (!Double.isFinite(centimetres) || centimetres <= 0.0) {
            throw new IllegalArgumentException("Ultrasonic reading must be finite and greater than zero");
        }
        return centimetres;
    }
}
