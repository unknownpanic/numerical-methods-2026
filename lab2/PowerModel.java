package lab2;

public class PowerModel {
    public double multiplierA;
    public double exponentB;
    public double scaledMultiplierA;

    public PowerModel(double[] xNodes, double[] yValues) {
        int nodeCount = xNodes.length;
        double sumX = 0, sumY = 0, sumXX = 0, sumXY = 0;

        for (int i = 0; i < nodeCount; i++) {
            double logX = Math.log(xNodes[i]);
            double logY = Math.log(yValues[i]);
            sumX += logX;
            sumY += logY;
            sumXX += logX * logX;
            sumXY += logX * logY;
        }

        exponentB = (nodeCount * sumXY - sumX * sumY) / (nodeCount * sumXX - sumX * sumX);
        multiplierA = Math.exp((sumY - exponentB * sumX) / nodeCount);
        scaledMultiplierA = multiplierA * Math.pow(1000.0, exponentB);
    }

    public double evaluate(double targetX) {
        return multiplierA * Math.pow(targetX, exponentB);
    }

    public double evaluateScaled(double scaledTargetX) {
        return scaledMultiplierA * Math.pow(scaledTargetX, exponentB);
    }
}
