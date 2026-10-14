package lab4;

public class NumericalDifferentiator {
    public static double centralDifference(double x0, double h) {
        return (SoilMoistureModel.evaluateMoisture(x0 + h) - SoilMoistureModel.evaluateMoisture(x0 - h)) / (2.0 * h);
    }

    public static double absoluteError(double approximateValue, double exactValue) {
        return Math.abs(approximateValue - exactValue);
    }

    public static double rungeRomberg(double yH, double y2H) {
        return yH + (yH - y2H) / 3.0;
    }

    public static double aitkenExtrapolation(double yH, double y2H, double y4H) {
        double numerator = (y2H * y2H) - y4H * yH;
        double denominator = 2.0 * y2H - (y4H + yH);
        return numerator / denominator;
    }

    public static double estimateAccuracyOrder(double yH, double y2H, double y4H) {
        double ratio = Math.abs((y4H - y2H) / (y2H - yH));
        return (1.0 / Math.log(2.0)) * Math.log(ratio);
    }
}
