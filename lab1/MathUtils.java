package lab1;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MathUtils {
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371000;
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
                Math.cos(phi1) * Math.cos(phi2) *
                        Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);

        return 2 * earthRadius * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public static double[] solveTridiagonal(double[] lowerDiag, double[] mainDiag, double[] upperDiag, double[] rhs) {
        int n = mainDiag.length;
        double[] alpha = new double[n];
        double[] beta = new double[n];
        double[] solution = new double[n];

        alpha[0] = -upperDiag[0] / mainDiag[0];
        beta[0] = rhs[0] / mainDiag[0];

        for (int i = 1; i < n; i++) {
            double denominator = lowerDiag[i] * alpha[i - 1] + mainDiag[i];
            alpha[i] = -upperDiag[i] / denominator;
            beta[i] = (rhs[i] - lowerDiag[i] * beta[i - 1]) / denominator;
        }

        solution[n - 1] = beta[n - 1];
        for (int i = n - 2; i >= 0; i--) {
            solution[i] = alpha[i] * solution[i + 1] + beta[i];
        }

        return solution;
    }

    public static double[] generateLinearSpace(double start, double end, int count) {
        double[] result = new double[count];
        for (int i = 0; i < count; i++) {
            result[i] = start + (end - start) * i / (count - 1);
        }
        return result;
    }

    public static int[] getUniformIndices(int totalNodes, int neededCount) {
        int[] indices = new int[neededCount];
        for (int i = 0; i < neededCount; i++) {
            indices[i] = (int) Math.round((double) i * (totalNodes - 1) / (neededCount - 1));
        }
        return indices;
    }

    public static String getSkippedIndicesInfo(int totalNodes, int[] usedIndices) {
        Set<Integer> used = new HashSet<>();
        for (int v : usedIndices) {
            used.add(v);
        }

        List<Integer> skipped = new ArrayList<>();
        for (int i = 0; i < totalNodes; i++) {
            if (!used.contains(i)) {
                skipped.add(i);
            }
        }
        return skipped.toString();
    }

    public static double[] createSubset(double[] array, int[] indices) {
        double[] subset = new double[indices.length];
        for (int i = 0; i < indices.length; i++) {
            subset[i] = array[indices[i]];
        }
        return subset;
    }

    public static String format(double value, int decimals) {
        return String.format(Locale.US, "%." + decimals + "f", value);
    }
}
