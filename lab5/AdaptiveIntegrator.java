package lab5;

public class AdaptiveIntegrator {
    public static class AdaptiveResult {
        public final double integralValue;
        public final double absoluteError;
        public final int functionEvaluations;

        public AdaptiveResult(double integralValue, double absoluteError, int functionEvaluations) {
            this.integralValue = integralValue;
            this.absoluteError = absoluteError;
            this.functionEvaluations = functionEvaluations;
        }
    }

    public static AdaptiveResult integrate(double a, double b, double delta, double exactIntegral) {
        int[] evaluationsCounter = {3};
        double yLeft = ServerLoadModel.evaluateLoad(a);
        double mid = 0.5 * (a + b);
        double yMid = ServerLoadModel.evaluateLoad(mid);
        double yRight = ServerLoadModel.evaluateLoad(b);

        double integral = recursiveSimpson(a, b, yLeft, yMid, yRight, delta, evaluationsCounter);
        double error = Math.abs(integral - exactIntegral);
        return new AdaptiveResult(integral, error, evaluationsCounter[0]);
    }

    private static double recursiveSimpson(double left, double right,
                                           double yLeft, double yMid, double yRight,
                                           double delta, int[] evaluationsCounter) {
        double h = right - left;
        double mid = 0.5 * (left + right);
        double q1 = 0.5 * (left + mid);
        double q3 = 0.5 * (mid + right);

        double yQ1 = ServerLoadModel.evaluateLoad(q1);
        double yQ3 = ServerLoadModel.evaluateLoad(q3);
        evaluationsCounter[0] += 2;

        double i1 = (h / 6.0) * (yLeft + 4.0 * yMid + yRight);
        double i2Left = (h / 12.0) * (yLeft + 4.0 * yQ1 + yMid);
        double i2Right = (h / 12.0) * (yMid + 4.0 * yQ3 + yRight);
        double i2 = i2Left + i2Right;

        if (Math.abs(i1 - i2) <= delta) {
            return i2;
        }

        return recursiveSimpson(left, mid, yLeft, yQ1, yMid, delta, evaluationsCounter)
                + recursiveSimpson(mid, right, yMid, yQ3, yRight, delta, evaluationsCounter);
    }
}
