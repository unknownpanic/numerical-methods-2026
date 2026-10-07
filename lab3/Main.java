package lab3;

import java.awt.Color;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;
import util.Chart;

public class Main {
    public static void main(String[] args) throws Exception {
        String outputDir = "lab3/output";
        new File(outputDir).mkdirs();

        String dataPath = promptForDataPath();

        double[][] data;
        try {
            data = DataLoader.loadCsvData(dataPath);
        } catch (Exception e) {
            System.err.println("Помилка: не знайдено файл " + dataPath);
            return;
        }

        double[] xValues = data[0];
        double[] yValues = data[1];
        int nodeCount = xValues.length;

        saveAndPlotInitialData(xValues, yValues, outputDir);

        int unscaledMaxDegree = 4;
        double[][] unscaledCoefficients = runUnscaledAnalysis(xValues, yValues, unscaledMaxDegree);

        double centerX = (xValues[0] + xValues[nodeCount - 1]) / 2;
        double scaleX = (xValues[nodeCount - 1] - xValues[0]) / 2;
        int scaledMaxDegree = 10;

        double[][] scaledCoefficients = new double[scaledMaxDegree + 1][];
        double[] standardDeviations = runScaledAnalysis(xValues, yValues, scaledMaxDegree, centerX, scaleX, scaledCoefficients, outputDir);

        checkMethodsConsistency(xValues, unscaledCoefficients, scaledCoefficients, unscaledMaxDegree, centerX, scaleX);

        int optimalDegree = findOptimalDegree(standardDeviations, scaledMaxDegree);

        plotApproximationsAndErrors(xValues, yValues, unscaledMaxDegree, scaledCoefficients, centerX, scaleX, outputDir);
        runExtrapolation(xValues, yValues, scaledMaxDegree, optimalDegree, unscaledCoefficients, scaledCoefficients, centerX, scaleX, outputDir);
    }

    private static String promptForDataPath() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Лабораторна робота №3 (МНК найкращого квадратичного наближення)");
        System.out.print("Введіть шлях до файлу даних (натисніть Enter для стандарту lab3/resource/temps.csv): ");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? "lab3/resource/temps.csv" : input;
    }

    private static void saveAndPlotInitialData(double[] xValues, double[] yValues, String outputDir) throws Exception {
        int nodeCount = xValues.length;
        StringBuilder tabulationBuilder = new StringBuilder("Month;Temp\n");
        System.out.println("\nВхідні дані (" + nodeCount + " вузлів):");

        for (int i = 0; i < nodeCount; i++) {
            tabulationBuilder.append((int) xValues[i]).append(';').append((int) yValues[i]).append('\n');
        }

        System.out.println(Arrays.toString(yValues));
        Files.writeString(Paths.get(outputDir, "tabulation.txt"), tabulationBuilder.toString());

        new Chart("Середньомісячна температура (вихідні дані)", "Місяць", "Температура, °C")
                .line("", xValues, yValues, new Color(0x1f77b4), true)
                .points("Дані", xValues, yValues, new Color(0xd62728))
                .save(outputDir + "/01_data.png");
    }

    private static double[][] runUnscaledAnalysis(double[] xValues, double[] yValues, int maxDegree) {
        System.out.println("\nНормальна система та розв'язок методом Гауса (базис x^k, ρ_i = 1)");
        double[][] coefficients = new double[maxDegree + 1][];

        for (int degree = 1; degree <= maxDegree; degree++) {
            double[][] matrix = LeastSquaresFitter.buildNormalMatrix(xValues, degree);
            double[] vector = LeastSquaresFitter.buildNormalVector(xValues, yValues, degree);

            if (degree == 2) {
                System.out.println("Приклад для m = 2: матриця b_kl та вектор c_k");
                for (int k = 0; k <= degree; k++) {
                    StringBuilder rowBuilder = new StringBuilder();
                    for (int l = 0; l <= degree; l++) {
                        rowBuilder.append(String.format(Locale.US, "%14.1f", matrix[k][l]));
                    }
                    System.out.println(rowBuilder + " | " + String.format(Locale.US, "%12.1f", vector[k]));
                }
            }

            coefficients[degree] = GaussSolver.solve(matrix, vector);
            StringBuilder resultBuilder = new StringBuilder("m=" + degree + ": a = [");
            for (int k = 0; k <= degree; k++) {
                resultBuilder.append(k > 0 ? ", " : "").append(String.format(Locale.US, "%.8g", coefficients[degree][k]));
            }
            double deviation = LeastSquaresFitter.calculateStandardDeviation(xValues, yValues, coefficients[degree]);
            System.out.println(resultBuilder + "]   δ = " + MathUtils.format(deviation, 4));
        }

        return coefficients;
    }

    private static double[] runScaledAnalysis(double[] xValues, double[] yValues, int maxDegree, double centerX, double scaleX, double[][] scaledCoefficients, String outputDir) throws Exception {
        int nodeCount = xValues.length;
        double[] deviations = new double[maxDegree + 1];

        System.out.println("\nДисперсія δ(m) (змінна t = (x − " + MathUtils.format(centerX, 1) + ")/" + MathUtils.format(scaleX, 1) + ")");
        System.out.println(" m |    δ, °C  | Δ відносно попереднього");
        StringBuilder varianceCsvBuilder = new StringBuilder("m;delta\n");

        for (int degree = 1; degree <= maxDegree; degree++) {
            scaledCoefficients[degree] = LeastSquaresFitter.fitScaledPolynomial(xValues, yValues, degree, centerX, scaleX);
            double sumSquaredErrors = 0;

            for (int i = 0; i < nodeCount; i++) {
                double error = LeastSquaresFitter.evaluatePolynomial(scaledCoefficients[degree], (xValues[i] - centerX) / scaleX) - yValues[i];
                sumSquaredErrors += error * error;
            }

            deviations[degree] = Math.sqrt(sumSquaredErrors / nodeCount);
            String deltaPrevious = degree > 1 ? MathUtils.format(deviations[degree - 1] - deviations[degree], 4) : "-";
            System.out.println(String.format(Locale.US, "%2d | %9.4f | %s", degree, deviations[degree], deltaPrevious));
            varianceCsvBuilder.append(degree).append(';').append(MathUtils.format(deviations[degree], 6)).append('\n');
        }

        Files.writeString(Paths.get(outputDir, "variance.csv"), varianceCsvBuilder.toString());

        double[] degreesArray = new double[maxDegree];
        double[] deviationsArray = new double[maxDegree];
        for (int degree = 1; degree <= maxDegree; degree++) {
            degreesArray[degree - 1] = degree;
            deviationsArray[degree - 1] = deviations[degree];
        }

        new Chart("Залежність дисперсії δ від степеня многочлена m", "Степінь m", "δ, °C")
                .line("δ(m)", degreesArray, deviationsArray, new Color(0x1f77b4), false)
                .points("", degreesArray, deviationsArray, new Color(0xd62728))
                .save(outputDir + "/02_variance.png");

        return deviations;
    }

    private static void checkMethodsConsistency(double[] xValues, double[][] unscaledCoefficients, double[][] scaledCoefficients, int maxDegree, double centerX, double scaleX) {
        double maxDifference = 0;
        for (int degree = 1; degree <= maxDegree; degree++) {
            for (double x : xValues) {
                double unscaledValue = LeastSquaresFitter.evaluatePolynomial(unscaledCoefficients[degree], x);
                double scaledValue = LeastSquaresFitter.evaluatePolynomial(scaledCoefficients[degree], (x - centerX) / scaleX);
                maxDifference = Math.max(maxDifference, Math.abs(unscaledValue - scaledValue));
            }
        }
        System.out.println("Узгодженість значень многочленів у x та у t (m ≤ 4): max різниця = " + MathUtils.formatScientific(maxDifference));
    }

    private static int findOptimalDegree(double[] deviations, int maxDegree) {
        int optimalDegree = 1;
        for (int degree = 2; degree <= maxDegree; degree++) {
            if (deviations[degree] < deviations[optimalDegree]) {
                optimalDegree = degree;
            }
        }
        System.out.println("\nОптимальний степінь за мінімумом δ серед m = 1.." + maxDegree + ": m = " + optimalDegree + " (δ = " + MathUtils.format(deviations[optimalDegree], 4) + ")");
        return optimalDegree;
    }

    private static void plotApproximationsAndErrors(double[] xValues, double[] yValues, int maxDegreeToPlot, double[][] scaledCoefficients, double centerX, double scaleX, String outputDir) throws Exception {
        int nodeCount = xValues.length;
        int gridPoints = 300;
        double[] gridX = MathUtils.generateLinearSpace(xValues[0], xValues[nodeCount - 1], gridPoints);

        Chart approximationChart = new Chart("Апроксимація МНК многочленами степенів m = 1…4", "Місяць", "Температура, °C");
        approximationChart.points("Дані", xValues, yValues, Color.BLACK);

        Chart errorChart = new Chart("Похибка апроксимації ε_i = φ(x_i) − f_i у вузлах", "Місяць", "ε, °C");

        System.out.println("\nПохибка у вузлах ε_i = φ_m(x_i) − f_i");
        StringBuilder headerBuilder = new StringBuilder(" x | f_i  ");
        for (int degree = 1; degree <= maxDegreeToPlot; degree++) {
            headerBuilder.append(String.format("| m=%d     ", degree));
        }
        System.out.println(headerBuilder);

        StringBuilder errorsCsvBuilder = new StringBuilder("month;temp;e_m1;e_m2;e_m3;e_m4\n");
        double[][] nodeErrors = new double[maxDegreeToPlot + 1][nodeCount];

        for (int degree = 1; degree <= maxDegreeToPlot; degree++) {
            double[] gridY = new double[gridPoints];
            for (int i = 0; i < gridPoints; i++) {
                gridY[i] = LeastSquaresFitter.evaluatePolynomial(scaledCoefficients[degree], (gridX[i] - centerX) / scaleX);
            }
            approximationChart.line("m = " + degree, gridX, gridY, Chart.PALETTE[degree - 1], degree > 2);

            for (int i = 0; i < nodeCount; i++) {
                nodeErrors[degree][i] = LeastSquaresFitter.evaluatePolynomial(scaledCoefficients[degree], (xValues[i] - centerX) / scaleX) - yValues[i];
            }
            errorChart.line("m = " + degree, xValues, nodeErrors[degree], Chart.PALETTE[degree - 1], false);
        }

        for (int i = 0; i < nodeCount; i++) {
            StringBuilder rowBuilder = new StringBuilder(String.format(Locale.US, "%2d | %5.1f", (int) xValues[i], yValues[i]));
            errorsCsvBuilder.append((int) xValues[i]).append(';').append(yValues[i]);

            for (int degree = 1; degree <= maxDegreeToPlot; degree++) {
                rowBuilder.append(String.format(Locale.US, " | % 7.3f", nodeErrors[degree][i]));
                errorsCsvBuilder.append(';').append(MathUtils.format(nodeErrors[degree][i], 5));
            }
            System.out.println(rowBuilder);
            errorsCsvBuilder.append('\n');
        }

        Files.writeString(Paths.get(outputDir, "errors.csv"), errorsCsvBuilder.toString());
        approximationChart.save(outputDir + "/03_fits.png");
        errorChart.save(outputDir + "/04_errors.png");
    }

    private static void runExtrapolation(double[] xValues, double[] yValues, int maxDegree, int optimalDegree, double[][] unscaledCoefficients, double[][] scaledCoefficients, double centerX, double scaleX, String outputDir) throws Exception {
        double[] futureMonths = {25, 26, 27};
        System.out.println("\nПрогноз (екстраполяція) на місяці 25–27");
        System.out.println(" m | φ(25)   | φ(26)   | φ(27)");

        for (int degree = 1; degree <= maxDegree; degree++) {
            StringBuilder rowBuilder = new StringBuilder(String.format("%2d ", degree));
            for (double futureX : futureMonths) {
                rowBuilder.append(String.format(Locale.US, "| % 7.2f ", LeastSquaresFitter.evaluatePolynomial(scaledCoefficients[degree], (futureX - centerX) / scaleX)));
            }
            System.out.println(rowBuilder);
        }

        double[] extendedGridX = MathUtils.generateLinearSpace(xValues[0], 27, 400);
        double[] extendedGridY = new double[400];
        for (int i = 0; i < 400; i++) {
            extendedGridY[i] = LeastSquaresFitter.evaluatePolynomial(scaledCoefficients[optimalDegree], (extendedGridX[i] - centerX) / scaleX);
        }

        double[] futurePredictions = new double[3];
        for (int i = 0; i < 3; i++) {
            futurePredictions[i] = LeastSquaresFitter.evaluatePolynomial(scaledCoefficients[optimalDegree], (futureMonths[i] - centerX) / scaleX);
        }

        new Chart("Оптимальний многочлен m = " + optimalDegree + " та прогноз на 3 місяці", "Місяць", "Температура, °C")
                .line("φ_" + optimalDegree + "(x)", extendedGridX, extendedGridY, new Color(0x1f77b4), false)
                .points("Дані", xValues, yValues, Color.BLACK)
                .points("Прогноз (25–27)", futureMonths, futurePredictions, new Color(0xd62728))
                .save(outputDir + "/05_forecast.png");

        Chart extrapolationChart = new Chart("Екстраполяція многочленами різних степенів", "Місяць", "Температура, °C");
        extrapolationChart.points("Дані", xValues, yValues, Color.BLACK);

        for (int degree : new int[]{2, 4, 6, 8}) {
            double[] currentPredictions = new double[400];
            for (int i = 0; i < 400; i++) {
                currentPredictions[i] = LeastSquaresFitter.evaluatePolynomial(scaledCoefficients[degree], (extendedGridX[i] - centerX) / scaleX);
            }
            extrapolationChart.line("m = " + degree, extendedGridX, currentPredictions, Chart.PALETTE[degree / 2], degree > 2);
        }

        extrapolationChart.ylim(-40, 50).save(outputDir + "/06_extrapolation.png");
    }
}
