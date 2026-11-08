package io.github.nakib16k.tunnelvision;

/** Bounded consecutive camera failures; a valid new frame resets the counter. */
public final class CameraFailurePolicy {
    private final int limit;
    private int consecutiveFailures;

    public CameraFailurePolicy(int limit) {
        if (limit < 1) throw new IllegalArgumentException("Failure limit must be positive");
        this.limit = limit;
    }

    public boolean recordFailure() { return ++consecutiveFailures >= limit; }
    public void recordSuccess() { consecutiveFailures = 0; }
    public int getConsecutiveFailures() { return consecutiveFailures; }
}
