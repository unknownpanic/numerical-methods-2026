package lab1;

public class CubicSpline {
    public double[] nodes;
    public double[] values;
    public double[] steps;
    public double[] coefA, coefB, coefC, coefD;

    public double[] sysLower, sysMain, sysUpper, sysRhs;
    public double[] innerCoefC;
    public double maxResidual;

    public CubicSpline(double[] nodes, double[] values) {
        int nodeCount = nodes.length;
        int intervalCount = nodeCount - 1;
        this.nodes = nodes;
        this.values = values;

        steps = new double[intervalCount];
        for (int i = 0; i < intervalCount; i++) {
            steps[i] = nodes[i + 1] - nodes[i];
        }

        int m = intervalCount - 1;
        sysLower = new double[m];
        sysMain = new double[m];
        sysUpper = new double[m];
        sysRhs = new double[m];

        for (int i = 1; i <= intervalCount - 1; i++) {
            int k = i - 1;
            sysLower[k] = (i == 1) ? 0 : steps[i - 1];
            sysMain[k] = 2 * (steps[i - 1] + steps[i]);
            sysUpper[k] = (i == intervalCount - 1) ? 0 : steps[i];
            sysRhs[k] = 3 * ((values[i + 1] - values[i]) / steps[i] - (values[i] - values[i - 1]) / steps[i - 1]);
        }

        innerCoefC = MathUtils.solveTridiagonal(sysLower, sysMain, sysUpper, sysRhs);

        maxResidual = 0;
        for (int k = 0; k < m; k++) {
            double lhs = sysMain[k] * innerCoefC[k];
            if (k > 0) lhs += sysLower[k] * innerCoefC[k - 1];
            if (k < m - 1) lhs += sysUpper[k] * innerCoefC[k + 1];
            maxResidual = Math.max(maxResidual, Math.abs(lhs - sysRhs[k]));
        }

        coefC = new double[nodeCount];
        for (int i = 1; i <= intervalCount - 1; i++) {
            coefC[i] = innerCoefC[i - 1];
        }

        coefA = new double[intervalCount];
        coefB = new double[intervalCount];
        coefD = new double[intervalCount];

        for (int i = 0; i < intervalCount; i++) {
            coefA[i] = values[i];
            coefD[i] = (coefC[i + 1] - coefC[i]) / (3 * steps[i]);
            coefB[i] = (values[i + 1] - values[i]) / steps[i] - steps[i] * (2 * coefC[i] + coefC[i + 1]) / 3;
        }
    }

    private int findIntervalIndex(double target) {
        int low = 0;
        int high = nodes.length - 2;
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (nodes[mid] <= target) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    public double calculateValue(double target) {
        int i = findIntervalIndex(target);
        double dx = target - nodes[i];
        return coefA[i] + dx * (coefB[i] + dx * (coefC[i] + dx * coefD[i]));
    }

    public double calculateDerivative(double target) {
        int i = findIntervalIndex(target);
        double dx = target - nodes[i];
        return coefB[i] + dx * (2 * coefC[i] + 3 * coefD[i] * dx);
    }
}
