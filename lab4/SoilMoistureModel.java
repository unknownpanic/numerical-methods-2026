package lab4;

public class SoilMoistureModel {
    public static double evaluateMoisture(double t) {
        return 50.0 * Math.exp(-0.1 * t) + 5.0 * Math.sin(t);
    }

    public static double evaluateExactDerivative(double t) {
        return -5.0 * Math.exp(-0.1 * t) + 5.0 * Math.cos(t);
    }
}
