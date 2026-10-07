package lab3;

public class GaussSolver {
    public static double[] solve(double[][] matrix, double[] vector) {
        int size = vector.length;
        double[][] aMatrix = new double[size][];

        for (int i = 0; i < size; i++) {
            aMatrix[i] = matrix[i].clone();
        }
        double[] bVector = vector.clone();

        for (int k = 0; k < size; k++) {
            int pivotRow = k;
            for (int i = k + 1; i < size; i++) {
                if (Math.abs(aMatrix[i][k]) > Math.abs(aMatrix[pivotRow][k])) {
                    pivotRow = i;
                }
            }

            if (Math.abs(aMatrix[pivotRow][k]) < 1e-300) {
                throw new ArithmeticException("Матриця вироджена");
            }

            double[] tempRow = aMatrix[k];
            aMatrix[k] = aMatrix[pivotRow];
            aMatrix[pivotRow] = tempRow;

            double tempVal = bVector[k];
            bVector[k] = bVector[pivotRow];
            bVector[pivotRow] = tempVal;

            for (int i = k + 1; i < size; i++) {
                double factor = aMatrix[i][k] / aMatrix[k][k];
                for (int j = k; j < size; j++) {
                    aMatrix[i][j] -= factor * aMatrix[k][j];
                }
                bVector[i] -= factor * bVector[k];
            }
        }

        double[] solution = new double[size];
        for (int i = size - 1; i >= 0; i--) {
            double sum = bVector[i];
            for (int j = i + 1; j < size; j++) {
                sum -= aMatrix[i][j] * solution[j];
            }
            solution[i] = sum / aMatrix[i][i];
        }

        return solution;
    }
}
