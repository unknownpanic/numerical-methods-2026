package lab3;

public class LeastSquaresFitter {
    public static double[][] buildNormalMatrix(double[] xValues, int degree) {
        double[][] matrix = new double[degree + 1][degree + 1];
        for (int k = 0; k <= degree; k++) {
            for (int l = 0; l <= degree; l++) {
                double sum = 0;
                for (double x : xValues) {
                    sum += Math.pow(x, k + l);
                }
                matrix[k][l] = sum;
            }
        }
        return matrix;
    }

    public static double[] buildNormalVector(double[] xValues, double[] yValues, int degree) {
        double[] vector = new double[degree + 1];
        for (int k = 0; k <= degree; k++) {
            double sum = 0;
            for (int i = 0; i < xValues.length; i++) {
                sum += yValues[i] * Math.pow(xValues[i], k);
            }
            vector[k] = sum;
        }
        return vector;
    }

    public static double evaluatePolynomial(double[] coefficients, double x) {
        double result = 0;
        for (int i = coefficients.length - 1; i >= 0; i--) {
            result = result * x + coefficients[i];
        }
        return result;
    }

    public static double calculateStandardDeviation(double[] xValues, double[] yValues, double[] coefficients) {
        double sumSquaredErrors = 0;
        for (int i = 0; i < xValues.length; i++) {
            double error = evaluatePolynomial(coefficients, xValues[i]) - yValues[i];
            sumSquaredErrors += error * error;
        }
        return Math.sqrt(sumSquaredErrors / xValues.length);
    }

    public static double[] fitScaledPolynomial(double[] xValues, double[] yValues, int degree, double centerX, double scaleX) {
        double[] scaledX = new double[xValues.length];
        for (int i = 0; i < xValues.length; i++) {
            scaledX[i] = (xValues[i] - centerX) / scaleX;
        }
        double[][] matrix = buildNormalMatrix(scaledX, degree);
        double[] vector = buildNormalVector(scaledX, yValues, degree);
        return GaussSolver.solve(matrix, vector);
    }
}
