package lab5;

public class ServerLoadModel {
    public static double evaluateLoad(double x) {
        double periodicComponent = 20.0 * Math.sin(Math.PI * x / 12.0);
        double peakComponent = 5.0 * Math.exp(-0.2 * (x - 12.0) * (x - 12.0));
        return 50.0 + periodicComponent + peakComponent;
    }

    public static double evaluateExactIntegral(double a, double b) {
        double baseArea = 50.0 * (b - a);
        double periodicArea = (-240.0 / Math.PI) * (Math.cos(Math.PI * b / 12.0) - Math.cos(Math.PI * a / 12.0));
        double gaussianFullArea = 5.0 * Math.sqrt(Math.PI / 0.2) * erf(12.0 * Math.sqrt(0.2));
        return baseArea + periodicArea + gaussianFullArea;
    }

    private static double erf(double z) {
        if (z >= 5.3) {
            double t = z * z;
            double asymp = Math.exp(-t) / (z * Math.sqrt(Math.PI)) * (1.0 - 1.0 / (2.0 * t) + 3.0 / (4.0 * t * t));
            return 1.0 - asymp;
        }
        double t = 1.0 / (1.0 + 0.5 * Math.abs(z));
        double ans = 1.0 - t * Math.exp(-z * z - 1.26551223 +
                t * (1.00002368 +
                        t * (0.37409196 +
                                t * (0.09678418 +
                                        t * (-0.18628806 +
                                                t * (0.27886807 +
                                                        t * (-1.13520398 +
                                                                t * (1.48851587 +
                                                                        t * (-0.82215223 +
                                                                                t * 0.17087277)))))))));
        return z >= 0 ? ans : -ans;
    }
}
