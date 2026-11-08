package io.github.nakib16k.tunnelvision;

import java.util.Objects;
import java.util.function.LongSupplier;

/** A monotonic segment timer with an injectable nanosecond clock for tests. */
public final class SegmentTimer {
    private final LongSupplier nanosecondClock;
    private long startedAt;
    private boolean running;

    public SegmentTimer() {
        this(System::nanoTime);
    }

    public SegmentTimer(LongSupplier nanosecondClock) {
        this.nanosecondClock = Objects.requireNonNull(nanosecondClock);
    }

    public void startSegmentTimer() {
        if (running) {
            throw new IllegalStateException("Segment timer already running");
        }
        startedAt = nanosecondClock.getAsLong();
        running = true;
    }

    public double stopSegmentTimer() {
        if (!running) {
            throw new IllegalStateException("Segment timer is not running");
        }
        long endedAt = nanosecondClock.getAsLong();
        running = false;
        return Math.max(0L, endedAt - startedAt) / 1_000_000_000.0;
    }

    public boolean isRunning() {
        return running;
    }
}
