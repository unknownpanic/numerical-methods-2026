package lab4;

import java.util.Locale;

public class MathUtils {
    public static double[] generateLinearSpace(double start, double end, int count) {
        double[] result = new double[count];
        for (int i = 0; i < count; i++) {
            result[i] = start + (end - start) * i / (count - 1);
        }
        return result;
    }

    public static double[] generateLogSpace(int minExponent, int maxExponent, int pointsPerDecade) {
        int totalDecades = maxExponent - minExponent;
        int count = totalDecades * pointsPerDecade + 1;
        double[] result = new double[count];
        for (int i = 0; i < count; i++) {
            double exp = minExponent + (double) i / pointsPerDecade;
            result[i] = Math.pow(10.0, exp);
        }
        return result;
    }

    public static String format(double value, int decimals) {
        return String.format(Locale.US, "%." + decimals + "f", value);
    }

    public static String formatScientific(double value) {
        return String.format(Locale.US, "%.3e", value);
    }

    public static String formatStepCompact(double value) {
        String raw = String.format(Locale.US, "%.0e", value);
        return raw.replace("e-0", "e-").replace("e+0", "e").replace("e+", "e");
    }
}
