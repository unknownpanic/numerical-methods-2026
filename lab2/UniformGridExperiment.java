package lab2;

public class UniformGridExperiment {
    public int nodeCount;
    public double intervalStart;
    public double intervalEnd;
    public double step;

    public double[] originalNodes;
    public double[] scaledNodes;
    public double[] values;
    public double[] newtonCoefficients;
    public double[] finiteDifferences;

    public UniformGridExperiment(int nodeCount, double intervalStart, double intervalEnd, PowerModel referenceModel) {
        this.nodeCount = nodeCount;
        this.intervalStart = intervalStart;
        this.intervalEnd = intervalEnd;
        this.step = (intervalEnd - intervalStart) / (nodeCount - 1);

        this.originalNodes = MathUtils.generateLinearSpace(intervalStart, intervalEnd, nodeCount);
        this.scaledNodes = new double[nodeCount];
        this.values = new double[nodeCount];

        for (int i = 0; i < nodeCount; i++) {
            this.scaledNodes[i] = originalNodes[i] / 1000.0;
            this.values[i] = referenceModel.evaluateScaled(this.scaledNodes[i]);
        }

        this.newtonCoefficients = PolynomialMath.getNewtonCoefficients(this.scaledNodes, this.values);
        this.finiteDifferences = PolynomialMath.computeFiniteDifferences(this.values);
    }

    public double evaluateNewton(double targetX) {
        return PolynomialMath.evaluateNewtonPolynomial(scaledNodes, newtonCoefficients, nodeCount - 1, targetX / 1000.0);
    }

    public double evaluateFactorial(double targetX) {
        return PolynomialMath.evaluateFactorialPolynomial(finiteDifferences, nodeCount - 1, (targetX - intervalStart) / step);
    }
}
