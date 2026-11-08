package io.github.nakib16k.tunnelvision;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import com.hopding.jrpicam.exceptions.FailedToRunRaspistillException;
import swiftbot.SwiftBotAPI;
import swiftbot.SwiftBotAPI.ImageSize;

/** Device adapter. This class is NOT instantiated by the offline test suite. */
public final class SwiftBotRobotAdapter implements RobotPort {
    private final SwiftBotAPI api;

    public SwiftBotRobotAdapter() { this.api = new SwiftBotAPI(); }

    @Override public double readUltrasonicDistanceCm() { return api.useUltrasound(); }
    @Override public void startMove(int leftWheel, int rightWheel) { api.startMove(leftWheel, rightWheel); }
    @Override public void stopMove() { api.stopMove(); }
    @Override public boolean isStopButtonReleased() { return api.BUTTON_X.isHigh(); }

    @Override public void setRedUnderlights() throws IOException {
        api.fillUnderlights(100, 0, 0);
        api.updateUnderlights();
    }

    @Override public BufferedImage captureGrayscale(Path directory) throws IOException, InterruptedException {
        try {
            // SwiftBot API returns the captured frame and writes it to the selected directory.
            return api.takeGrayscaleStill(directory.toString(), "tempImg", ImageSize.SQUARE_48x48);
        } catch (FailedToRunRaspistillException e) {
            throw new IOException("SwiftBot camera command failed", e);
        }
    }

    @Override public void disableUnderlights() { api.disableUnderlights(); }
    @Override public void shutdown() { api.shutdown(); }
}
