package io.github.nakib16k.tunnelvision;

import java.awt.image.BufferedImage;

/** Pure brightness calculation; does not require the SwiftBot camera. */
public final class ImageIntensityAnalyzer {
    private ImageIntensityAnalyzer() { }

    /**
     * Return the rounded mean of R, G and B for every pixel (0..255).
     * Read each pixel before extracting its channels; the original 2023
     * implementation mistakenly extracted channels from the previous pixel.
     */
    public static int calculateAverageIntensity(BufferedImage image) {
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            throw new IllegalArgumentException("A nonempty image is required");
        }
        long sum = 0L;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                sum += (rgb >>> 16) & 0xff;
                sum += (rgb >>> 8) & 0xff;
                sum += rgb & 0xff;
            }
        }
        long channelCount = 3L * image.getWidth() * image.getHeight();
        return (int) Math.round((double) sum / channelCount);
    }
}
