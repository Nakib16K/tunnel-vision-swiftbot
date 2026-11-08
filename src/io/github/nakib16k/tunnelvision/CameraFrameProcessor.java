package io.github.nakib16k.tunnelvision;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Calculates brightness only from a freshly returned camera frame (never stale disk content). */
public final class CameraFrameProcessor {
    private CameraFrameProcessor() { }

    public static int captureIntensity(RobotPort robot, Path imageDirectory)
            throws IOException, InterruptedException {
        Objects.requireNonNull(robot, "robot");
        Objects.requireNonNull(imageDirectory, "imageDirectory");
        BufferedImage frame = robot.captureGrayscale(imageDirectory);
        if (frame == null) {
            throw new IOException("Camera returned no image; refusing to reuse a previous frame");
        }
        try {
            if (frame.getWidth() <= 0 || frame.getHeight() <= 0) {
                throw new IOException("Camera returned an empty frame");
            }
            return ImageIntensityAnalyzer.calculateAverageIntensity(frame);
        } catch (RuntimeException ex) {
            throw new IOException("Camera frame could not be processed", ex);
        } finally {
            frame.flush();
        }
    }
}
