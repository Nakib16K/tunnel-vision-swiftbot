package io.github.nakib16k.tunnelvision;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.List;
import java.util.Objects;

/** Formats an entire or partial journey without opening files or using hardware. */
public final class JourneyReport {
    private JourneyReport() { }

    public static String format(List<Double> tunnelLengths, List<Double> tunnelTimes,
                                List<Double> gapLengths, List<Double> gapTimes, String dateText) {
        Objects.requireNonNull(tunnelLengths);
        Objects.requireNonNull(tunnelTimes);
        Objects.requireNonNull(gapLengths);
        Objects.requireNonNull(gapTimes);
        Objects.requireNonNull(dateText);
        if (tunnelLengths.size() != tunnelTimes.size() || gapLengths.size() != gapTimes.size()) {
            throw new IllegalArgumentException("Each length needs a corresponding duration");
        }
        DecimalFormat fmt = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.ROOT));
        StringBuilder report = new StringBuilder("LOG\n\n").append(dateText).append("\n\n");
        double totalDistance = 0.0;
        double totalDuration = 0.0;
        int maxIndex = -1;
        int minIndex = -1;

        if (tunnelLengths.isEmpty()) {
            report.append("No tunnel measurements recorded.\n");
        }
        for (int i = 0; i < tunnelLengths.size(); i++) {
            double length = requiredNonnegative(tunnelLengths.get(i), "Tunnel length");
            double duration = requiredNonnegative(tunnelTimes.get(i), "Tunnel duration");
            report.append("Length of Tunnel ").append(i + 1).append(" = ")
                    .append(fmt.format(length)).append("cm\n");
            totalDistance += length;
            totalDuration += duration;
            // Equal extrema consistently select the earliest tunnel.
            if (maxIndex < 0 || length > tunnelLengths.get(maxIndex)) maxIndex = i;
            if (minIndex < 0 || length < tunnelLengths.get(minIndex)) minIndex = i;
        }
        if (gapLengths.isEmpty()) {
            report.append("No gap measurements recorded.\n");
        }
        for (int i = 0; i < gapLengths.size(); i++) {
            double length = requiredNonnegative(gapLengths.get(i), "Gap length");
            double duration = requiredNonnegative(gapTimes.get(i), "Gap duration");
            report.append("The Distance of Gap number: ").append(i + 1)
                    .append(" between two consecutive tunnels = ")
                    .append(fmt.format(length)).append("cm\n");
            totalDistance += length;
            totalDuration += duration;
        }
        report.append("The Total Distance = ").append(fmt.format(totalDistance)).append("cm\n");
        report.append("The Total Duration = ").append(fmt.format(totalDuration)).append("s\n");
        if (tunnelLengths.size() > 1) {
            report.append("The Tunnel: ").append(maxIndex + 1)
                    .append(" of Length = ").append(fmt.format(tunnelLengths.get(maxIndex)))
                    .append("cm is Maximum\n");
            report.append("The Tunnel: ").append(minIndex + 1)
                    .append(" of Length = ").append(fmt.format(tunnelLengths.get(minIndex)))
                    .append("cm is Minimum\n");
        }
        if (!Double.isFinite(totalDistance) || !Double.isFinite(totalDuration)) {
            throw new IllegalArgumentException("Journey totals are not finite");
        }
        return report.toString();
    }

    private static double requiredNonnegative(Double value, String name) {
        if (value == null || !Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be finite and nonnegative");
        }
        return value;
    }
}
