package io.github.nakib16k.tunnelvision;

/**
 * Light-based tunnel transition detector. Starts inside the first tunnel,
 * as in the 2023 implementation. A directional brightness change (>5)
 * followed by stabilisation (absolute change <=20) confirms a transition.
 * A strong reversal cancels a pending transition. Hardware thresholds remain
 * those from the original coursework and have NOT been physically retested.
 */
public final class TunnelStateDetector {
    public enum State { INSIDE_TUNNEL, OUTSIDE_TUNNEL }
    public enum Transition { NONE, EXITED_TUNNEL, ENTERED_TUNNEL }

    private State state = State.INSIDE_TUNNEL;
    private Integer previousIntensity;
    private boolean awaitingStabilisation;
    private static final int CHANGE_THRESHOLD = 5;
    private static final int STABILISATION_THRESHOLD = 20;

    public State getState() {
        return state;
    }

    /** Lost camera frames invalidate a pending transition, not the known physical state. */
    public void resetAfterLostFrame() {
        previousIntensity = null;
        awaitingStabilisation = false;
    }

    public Transition acceptIntensity(int currentIntensity) {
        if (currentIntensity < 0 || currentIntensity > 255) {
            throw new IllegalArgumentException("Intensity must be between 0 and 255");
        }
        if (previousIntensity == null) {
            previousIntensity = currentIntensity;
            return Transition.NONE;
        }
        int delta = currentIntensity - previousIntensity;
        previousIntensity = currentIntensity;

        if (awaitingStabilisation) {
            boolean reversed = state == State.INSIDE_TUNNEL
                    ? delta < -CHANGE_THRESHOLD : delta > CHANGE_THRESHOLD;
            if (reversed) {
                awaitingStabilisation = false;
                return Transition.NONE;
            }
            if (Math.abs(delta) <= STABILISATION_THRESHOLD) {
                awaitingStabilisation = false;
                if (state == State.INSIDE_TUNNEL) {
                    state = State.OUTSIDE_TUNNEL;
                    return Transition.EXITED_TUNNEL;
                }
                state = State.INSIDE_TUNNEL;
                return Transition.ENTERED_TUNNEL;
            }
            return Transition.NONE;
        }
        if ((state == State.INSIDE_TUNNEL && delta > CHANGE_THRESHOLD)
                || (state == State.OUTSIDE_TUNNEL && delta < -CHANGE_THRESHOLD)) {
            awaitingStabilisation = true;
        }
        return Transition.NONE;
    }
}
