package lab3;

import java.util.Locale;

public class MathUtils {
    public static double[] generateLinearSpace(double start, double end, int count) {
        double[] result = new double[count];
        for (int i = 0; i < count; i++) {
            result[i] = start + (end - start) * i / (count - 1);
        }
        return result;
    }

    public static String format(double value, int decimals) {
        return String.format(Locale.US, "%." + decimals + "f", value);
    }

    public static String formatScientific(double value) {
        return String.format(Locale.US, "%.3e", value);
    }
}