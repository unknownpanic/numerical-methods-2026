package lab2;

public class PolynomialMath {
    public static double[][] computeDividedDifferencesTable(double[] xNodes, double[] yValues) {
        int nodeCount = xNodes.length;
        double[][] diffTable = new double[nodeCount][];
        diffTable[0] = yValues.clone();

        for (int k = 1; k < nodeCount; k++) {
            diffTable[k] = new double[nodeCount - k];
            for (int i = 0; i < nodeCount - k; i++) {
                diffTable[k][i] = (diffTable[k - 1][i + 1] - diffTable[k - 1][i]) / (xNodes[i + k] - xNodes[i]);
            }
        }
        return diffTable;
    }

    public static double computeDividedDifference(double[] xNodes, double[] yValues, int startIndex, int order) {
        if (order == 0) {
            return yValues[startIndex];
        }
        double diffNext = computeDividedDifference(xNodes, yValues, startIndex + 1, order - 1);
        double diffCurrent = computeDividedDifference(xNodes, yValues, startIndex, order - 1);
        return (diffNext - diffCurrent) / (xNodes[startIndex + order] - xNodes[startIndex]);
    }

    public static double[] getNewtonCoefficients(double[] xNodes, double[] yValues) {
        double[][] diffTable = computeDividedDifferencesTable(xNodes, yValues);
        double[] coefficients = new double[xNodes.length];
        for (int k = 0; k < xNodes.length; k++) {
            coefficients[k] = diffTable[k][0];
        }
        return coefficients;
    }

    public static double evaluateNewtonPolynomial(double[] xNodes, double[] coefficients, int degree, double targetX) {
        double result = coefficients[degree];
        for (int k = degree - 1; k >= 0; k--) {
            result = coefficients[k] + (targetX - xNodes[k]) * result;
        }
        return result;
    }

    public static double[] computeFiniteDifferences(double[] yValues) {
        int nodeCount = yValues.length;
        double[] currentDiffs = yValues.clone();
        double[] leadingDiffs = new double[nodeCount];
        leadingDiffs[0] = currentDiffs[0];

        for (int k = 1; k < nodeCount; k++) {
            double[] nextDiffs = new double[nodeCount - k];
            for (int i = 0; i < nodeCount - k; i++) {
                nextDiffs[i] = currentDiffs[i + 1] - currentDiffs[i];
            }
            leadingDiffs[k] = nextDiffs[0];
            currentDiffs = nextDiffs;
        }
        return leadingDiffs;
    }

    public static double evaluateFactorialPolynomial(double[] finiteDiffs, int degree, double pTarget) {
        double sum = 0;
        double factorial = 1;
        double fallingFactorial = 1;

        for (int k = 0; k <= degree; k++) {
            if (k > 0) {
                factorial *= k;
                fallingFactorial *= (pTarget - (k - 1));
            }
            sum += finiteDiffs[k] / factorial * fallingFactorial;
        }
        return sum;
    }

    public static double evaluateLagrangePolynomial(double[] xNodes, double[] yValues, double targetX) {
        double sum = 0;
        for (int i = 0; i < xNodes.length; i++) {
            double basisPolynomial = 1;
            for (int j = 0; j < xNodes.length; j++) {
                if (j != i) {
                    basisPolynomial *= (targetX - xNodes[j]) / (xNodes[i] - xNodes[j]);
                }
            }
            sum += yValues[i] * basisPolynomial;
        }
        return sum;
    }
}
