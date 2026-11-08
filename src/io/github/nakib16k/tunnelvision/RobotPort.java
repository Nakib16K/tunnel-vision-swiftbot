package io.github.nakib16k.tunnelvision;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/** Minimal robot-facing seam for offline testing; SwiftBot API is used only in its adapter. */
public interface RobotPort {
    double readUltrasonicDistanceCm();
    void startMove(int leftWheel, int rightWheel);
    void stopMove();
    boolean isStopButtonReleased();
    void setRedUnderlights() throws IOException;
    BufferedImage captureGrayscale(Path directory) throws IOException, InterruptedException;
    void disableUnderlights();
    void shutdown();
}
