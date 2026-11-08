package io.github.nakib16k.tunnelvision;

/** Requirements from the 2023 assignment: minimum 30 cm tunnel,
 * minimum 20 cm gap, consecutive tunnel difference of 10–15 cm inclusive. */
public final class DistanceChecks {
    public static final double MIN_TUNNEL_CM = 30.0;
    public static final double MIN_GAP_CM = 20.0;
    public static final double MIN_VARIATION_CM = 10.0;
    public static final double MAX_VARIATION_CM = 15.0;

    private DistanceChecks() { }

    public static double calculateDistance(double speedCmPerSecond, double elapsedSeconds) {
        if (!Double.isFinite(speedCmPerSecond) || speedCmPerSecond < 0.0
                || !Double.isFinite(elapsedSeconds) || elapsedSeconds < 0.0) {
            throw new IllegalArgumentException("Speed and time must be finite and nonnegative");
        }
        double distance = speedCmPerSecond * elapsedSeconds;
        if (!Double.isFinite(distance)) {
            throw new IllegalArgumentException("Distance calculation overflow");
        }
        return distance;
    }

    public static boolean isValidTunnel(double lengthCm, Double previousLengthCm) {
        if (!Double.isFinite(lengthCm) || lengthCm < MIN_TUNNEL_CM) {
            return false;
        }
        if (previousLengthCm == null) {
            return true;
        }
        if (!Double.isFinite(previousLengthCm)) {
            return false;
        }
        double variation = Math.abs(lengthCm - previousLengthCm);
        return variation >= MIN_VARIATION_CM && variation <= MAX_VARIATION_CM;
    }

    public static boolean isValidGap(double lengthCm) {
        return Double.isFinite(lengthCm) && lengthCm >= MIN_GAP_CM;
    }
}
