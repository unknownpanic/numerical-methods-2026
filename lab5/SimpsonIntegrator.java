package lab5;

public class SimpsonIntegrator {
    public static double integrate(double a, double b, int n) {
        if (n <= 0 || n % 2 != 0) {
            throw new IllegalArgumentException("Число розбиттів N повинно бути додатним парним числом");
        }
        double h = (b - a) / n;
        double sumOdd = 0.0;
        double sumEven = 0.0;

        for (int i = 1; i < n; i += 2) {
            sumOdd += ServerLoadModel.evaluateLoad(a + i * h);
        }
        for (int i = 2; i < n; i += 2) {
            sumEven += ServerLoadModel.evaluateLoad(a + i * h);
        }

        double endpoints = ServerLoadModel.evaluateLoad(a) + ServerLoadModel.evaluateLoad(b);
        return (h / 3.0) * (endpoints + 4.0 * sumOdd + 2.0 * sumEven);
    }

    public static double absoluteError(double approximateValue, double exactValue) {
        return Math.abs(approximateValue - exactValue);
    }

    public static double rungeRomberg(double integralN0, double integralN0Half) {
        return integralN0 + (integralN0 - integralN0Half) / 15.0;
    }

    public static double aitkenExtrapolation(double integralN0, double integralN0Half, double integralN0Quarter) {
        double numerator = (integralN0Half * integralN0Half) - integralN0 * integralN0Quarter;
        double denominator = 2.0 * integralN0Half - (integralN0 + integralN0Quarter);
        return numerator / denominator;
    }

    public static double estimateAccuracyOrder(double integralN0, double integralN0Half, double integralN0Quarter) {
        double ratio = Math.abs((integralN0Quarter - integralN0Half) / (integralN0Half - integralN0));
        return (1.0 / Math.log(2.0)) * Math.log(ratio);
    }
}
