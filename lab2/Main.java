package lab2;

import java.awt.Color;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;
import util.Chart;

public class Main {
    public static void main(String[] args) throws Exception {
        String outputDir = "lab2/output";
        new File(outputDir).mkdirs();

        Scanner scanner = new Scanner(System.in);
        System.out.println("Лабораторна робота №2 (Варіант 3: Прогноз часу тренування ML)");
        System.out.print("Введіть розмір датасету для прогнозу (натисніть Enter для стандарту 120000): ");
        String input = scanner.nextLine().trim();
        double predictionTarget = input.isEmpty() ? 120000 : Double.parseDouble(input);

        double[][] data;
        try {
            data = DataLoader.loadCsvData("lab2/resource/data.csv");
        } catch (Exception e) {
            System.err.println("Помилка: не знайдено файл lab2/resource/data.csv");
            return;
        }

        double[] datasetSizes = data[0];
        double[] trainingTimes = data[1];
        PowerModel referenceModel = new PowerModel(datasetSizes, trainingTimes);

        runExperimentalDataAnalysis(datasetSizes, trainingTimes, predictionTarget, referenceModel, outputDir);

        double intervalStart = 10000;
        double intervalEnd = 160000;

        runUniformGridAnalysis(intervalStart, intervalEnd, predictionTarget, referenceModel, outputDir);
        runStepInfluenceStudy(intervalStart, intervalEnd, referenceModel, outputDir);
        runIntervalInfluenceStudy(intervalStart, referenceModel, outputDir);
        runNoiseInfluenceStudy(intervalStart, intervalEnd, predictionTarget, referenceModel, outputDir);
        runLagrangeComparison(intervalStart, intervalEnd, predictionTarget, datasetSizes, trainingTimes, referenceModel);
    }

    private static void runExperimentalDataAnalysis(double[] xNodes, double[] yValues, double target, PowerModel referenceModel, String outputDir) throws Exception {
        int nodeCount = xNodes.length;
        System.out.println("\nЧАСТИНА A. Експериментальні дані (варіант 3)");
        System.out.println("Вузли: " + Arrays.toString(xNodes));
        System.out.println("Значення: " + Arrays.toString(yValues));

        double[][] dividedDifferences = PolynomialMath.computeDividedDifferencesTable(xNodes, yValues);
        System.out.println("\nТаблиця розділених різниць (у змінній N):");
        System.out.println("   N      |  f[x_i]  | f[x_i,x_i+1] | f[..2]      | f[..3]      | f[..4]");
        for (int i = 0; i < nodeCount; i++) {
            StringBuilder rowOutput = new StringBuilder(String.format(Locale.US, "%8.0f", xNodes[i]));
            for (int k = 0; k < nodeCount; k++) {
                rowOutput.append(k < nodeCount - i ? String.format(Locale.US, " | % .6e", dividedDifferences[k][i]) : " |            ");
            }
            System.out.println(rowOutput);
        }

        double maxDiffDiscrepancy = 0;
        for (int k = 0; k < nodeCount; k++) {
            for (int i = 0; i < nodeCount - k; i++) {
                maxDiffDiscrepancy = Math.max(maxDiffDiscrepancy, Math.abs(PolynomialMath.computeDividedDifference(xNodes, yValues, i, k) - dividedDifferences[k][i]));
            }
        }
        System.out.println("Перевірка рекурентної функції проти таблиці: " + MathUtils.formatScientific(maxDiffDiscrepancy));

        double[] newtonCoefs = PolynomialMath.getNewtonCoefficients(xNodes, yValues);
        System.out.println("\nКоефіцієнти Ньютона f[x_0..x_k]: " + Arrays.toString(newtonCoefs));
        System.out.println("\nПрогноз P_n(" + target + ") многочленом Ньютона за змінною N:");

        double[] newtonPredictions = new double[nodeCount];
        for (int n = 1; n < nodeCount; n++) {
            newtonPredictions[n] = PolynomialMath.evaluateNewtonPolynomial(xNodes, newtonCoefs, n, target);
            System.out.println("  степінь n=" + n + " (перші " + (n + 1) + " вузлів): P = " + MathUtils.format(newtonPredictions[n], 4) + " c");
        }

        double[] pNodes = new double[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            pNodes[i] = Math.log(xNodes[i] / xNodes[0]) / Math.log(2);
        }
        double pTarget = Math.log(target / xNodes[0]) / Math.log(2);
        System.out.println("\nЗамiна змінної p = log2(N/10000): вузли p = " + Arrays.toString(pNodes) + ", p* = " + MathUtils.format(pTarget, 5));

        double[] finiteDiffs = PolynomialMath.computeFiniteDifferences(yValues);
        System.out.println("Скінченні різниці Δ^k y_0: " + Arrays.toString(finiteDiffs));

        double[] pNewtonCoefs = PolynomialMath.getNewtonCoefficients(pNodes, yValues);
        double maxMethodDiscrepancy = 0;

        System.out.println("\nПрогноз факторіальним многочленом (ряд за p^(k)) та Ньютоном у змінній p:");
        for (int n = 1; n < nodeCount; n++) {
            double factorialPrediction = PolynomialMath.evaluateFactorialPolynomial(finiteDiffs, n, pTarget);
            double pNewtonPrediction = PolynomialMath.evaluateNewtonPolynomial(pNodes, pNewtonCoefs, n, pTarget);
            maxMethodDiscrepancy = Math.max(maxMethodDiscrepancy, Math.abs(factorialPrediction - pNewtonPrediction));
            System.out.println("  n=" + n + ": факторіальний = " + MathUtils.format(factorialPrediction, 4) + " c;  Ньютон(p) = " + MathUtils.format(pNewtonPrediction, 4) + " c");
        }
        System.out.println("Розбіжність факторіального і Ньютона(p): " + MathUtils.formatScientific(maxMethodDiscrepancy));

        System.out.println("\nДовідкова модель t = A*N^B: A = " + MathUtils.formatScientific(referenceModel.multiplierA) +
                ", B = " + MathUtils.format(referenceModel.exponentB, 4) +
                ";  t(" + target + ") = " + MathUtils.format(referenceModel.evaluate(target), 3) + " c");

        for (int i = 0; i < nodeCount; i++) {
            System.out.println(String.format(Locale.US, "   N=%6.0f: t=%6.1f, модель=%7.2f", xNodes[i], yValues[i], referenceModel.evaluate(xNodes[i])));
        }

        int gridPoints = 400;
        double[] gridX = MathUtils.generateLinearSpace(xNodes[0], xNodes[nodeCount - 1], gridPoints);
        double[] gridNewton = new double[gridPoints];
        double[] gridFactorial = new double[gridPoints];
        double[] gridModel = new double[gridPoints];

        for (int i = 0; i < gridPoints; i++) {
            gridNewton[i] = PolynomialMath.evaluateNewtonPolynomial(xNodes, newtonCoefs, nodeCount - 1, gridX[i]);
            gridFactorial[i] = PolynomialMath.evaluateFactorialPolynomial(finiteDiffs, nodeCount - 1, Math.log(gridX[i] / xNodes[0]) / Math.log(2));
            gridModel[i] = referenceModel.evaluate(gridX[i]);
        }

        new Chart("Час тренування: інтерполяція за 5 вузлами", "Розмір датасету", "Час, с")
                .line("Ньютон (змінна N)", gridX, gridNewton, new Color(0x1f77b4), false)
                .line("Факторіальний (p = log2(N/10000))", gridX, gridFactorial, new Color(0x2ca02c), true)
                .line("Степенева модель (довідково)", gridX, gridModel, Color.GRAY, true)
                .points("Експериментальні дані", xNodes, yValues, new Color(0xd62728))
                .points("Прогноз", new double[]{target}, new double[]{newtonPredictions[nodeCount - 1]}, Color.BLACK)
                .save(outputDir + "/01_data_prediction.png");
    }

    private static void runUniformGridAnalysis(double intervalStart, double intervalEnd, double target, PowerModel referenceModel, String outputDir) throws Exception {
        System.out.println("\nЧАСТИНА B. Дослідження на рівномірній сітці (еталон — степенева модель)");

        int gridPoints = 1501;
        double[] referenceGridX = MathUtils.generateLinearSpace(intervalStart, intervalEnd, gridPoints);
        double[] referenceGridY = new double[gridPoints];
        for (int i = 0; i < gridPoints; i++) {
            referenceGridY[i] = referenceModel.evaluate(referenceGridX[i]);
        }

        int[] nodeCountsToTest = {5, 10, 20};
        Chart functionChart = new Chart("Інтерполяційні многочлени Ньютона на рівномірній сітці", "Розмір датасету", "Час, с");
        Chart errorChart = new Chart("Похибка |f(N) − P_n(N)|", "Розмір датасету", "Похибка, с");
        functionChart.line("f(N) — еталонна модель", referenceGridX, referenceGridY, Color.BLACK, false);

        System.out.println("\n n  |    h     | max|f-P| (Ньютон) | max|f-P| (фактор.) | max|Ньютон-фактор.| | |f-P| у цільовій | Прогноз");
        StringBuilder csvOutput = new StringBuilder("n;h;max_err_newton;max_err_factorial;diff;err_target;P_target\n");

        for (int q = 0; q < nodeCountsToTest.length; q++) {
            int n = nodeCountsToTest[q];
            UniformGridExperiment experiment = new UniformGridExperiment(n, intervalStart, intervalEnd, referenceModel);

            double[] approxNewton = new double[gridPoints];
            double[] approxFactorial = new double[gridPoints];
            double[] newtonErrors = new double[gridPoints];

            double maxNewtonError = 0;
            double maxFactorialError = 0;
            double maxDifference = 0;

            for (int i = 0; i < gridPoints; i++) {
                approxNewton[i] = experiment.evaluateNewton(referenceGridX[i]);
                approxFactorial[i] = experiment.evaluateFactorial(referenceGridX[i]);
                newtonErrors[i] = Math.abs(referenceGridY[i] - approxNewton[i]);

                maxNewtonError = Math.max(maxNewtonError, newtonErrors[i]);
                maxFactorialError = Math.max(maxFactorialError, Math.abs(referenceGridY[i] - approxFactorial[i]));
                maxDifference = Math.max(maxDifference, Math.abs(approxNewton[i] - approxFactorial[i]));
            }

            double prediction = experiment.evaluateNewton(target);
            double targetError = Math.abs(referenceModel.evaluate(target) - prediction);

            System.out.println(String.format(Locale.US, "%3d | %8.1f | %18s | %18s | %18s | %14s | %.4f",
                    n, experiment.step, MathUtils.formatScientific(maxNewtonError), MathUtils.formatScientific(maxFactorialError),
                    MathUtils.formatScientific(maxDifference), MathUtils.formatScientific(targetError), prediction));

            csvOutput.append(String.format(Locale.US, "%d;%.2f;%.6e;%.6e;%.3e;%.6e;%.6f%n",
                    n, experiment.step, maxNewtonError, maxFactorialError, maxDifference, targetError, prediction));

            functionChart.line("P_" + (n - 1) + "(N), n=" + n, referenceGridX, approxNewton, Chart.PALETTE[q + 1], q > 0);
            errorChart.line("n = " + n, referenceGridX, newtonErrors, Chart.PALETTE[q + 1], false);

            StringBuilder tabulationOutput = new StringBuilder("N;f(N);P_Newton(N);P_factorial(N);|f-P|\n");
            for (int i = 0; i < gridPoints; i += 50) {
                tabulationOutput.append(String.format(Locale.US, "%.1f;%.6f;%.6f;%.6f;%.3e%n",
                        referenceGridX[i], referenceGridY[i], approxNewton[i], approxFactorial[i], newtonErrors[i]));
            }
            Files.writeString(Paths.get(outputDir, "tab_n" + n + ".csv"), tabulationOutput.toString());

            StringBuilder nodesOutput = new StringBuilder("N_i;f(N_i)\n");
            for (int i = 0; i < n; i++) {
                nodesOutput.append(String.format(Locale.US, "%.2f;%.6f%n", experiment.originalNodes[i], experiment.values[i]));
            }
            Files.writeString(Paths.get(outputDir, "nodes_n" + n + ".csv"), nodesOutput.toString());
        }

        Files.writeString(Paths.get(outputDir, "uniform_errors.csv"), csvOutput.toString());
        functionChart.ylim(-200, 900).save(outputDir + "/02_uniform_polys.png");
        errorChart.save(outputDir + "/03_uniform_errors.png");
    }

    private static void runStepInfluenceStudy(double intervalStart, double intervalEnd, PowerModel referenceModel, String outputDir) throws Exception {
        System.out.println("\nДослідження 1: фіксований інтервал [10000;160000], зростання числа вузлів");
        System.out.println(" n  |    h     | max|f-P|    | max на краях (по 10% довжини) | max у центрі");

        int[] nodeCounts = {3, 4, 5, 6, 8, 10, 12, 15, 20, 25, 30};
        double[] xData = new double[nodeCounts.length];
        double[] yData = new double[nodeCounts.length];

        for (int q = 0; q < nodeCounts.length; q++) {
            UniformGridExperiment experiment = new UniformGridExperiment(nodeCounts[q], intervalStart, intervalEnd, referenceModel);
            double maxError = 0, edgeError = 0, centerError = 0;

            double[] gridX = MathUtils.generateLinearSpace(intervalStart, intervalEnd, 1501);
            for (int i = 0; i < 1501; i++) {
                double error = Math.abs(referenceModel.evaluate(gridX[i]) - experiment.evaluateNewton(gridX[i]));
                maxError = Math.max(maxError, error);

                double relativePosition = (gridX[i] - intervalStart) / (intervalEnd - intervalStart);
                if (relativePosition < 0.1 || relativePosition > 0.9) edgeError = Math.max(edgeError, error);
                if (relativePosition > 0.4 && relativePosition < 0.6) centerError = Math.max(centerError, error);
            }

            xData[q] = nodeCounts[q];
            yData[q] = maxError;
            System.out.println(String.format(Locale.US, "%3d | %8.1f | %11s | %29s | %s",
                    nodeCounts[q], experiment.step, MathUtils.formatScientific(maxError),
                    MathUtils.formatScientific(edgeError), MathUtils.formatScientific(centerError)));
        }

        new Chart("Максимальна похибка залежно від числа вузлів (інтервал фіксований)", "Число вузлів n", "max |f − P|, с")
                .line("max похибка", xData, yData, new Color(0xd62728), false)
                .points("", xData, yData, new Color(0xd62728))
                .save(outputDir + "/04_error_vs_nodes.png");
    }

    private static void runIntervalInfluenceStudy(double intervalStart, PowerModel referenceModel, String outputDir) throws Exception {
        System.out.println("\nДослідження 2: фіксований крок h = 10000, змінний інтервал [10000; 10000+(n-1)h]");
        System.out.println(" n  | інтервал            | max|f-P|   | max відносна похибка, %");

        int[] nodeCounts = {3, 5, 8, 10, 12, 15, 20};
        double[] xData = new double[nodeCounts.length];
        double[] yData = new double[nodeCounts.length];
        double fixedStep = 10000;

        for (int q = 0; q < nodeCounts.length; q++) {
            int n = nodeCounts[q];
            double currentEnd = intervalStart + (n - 1) * fixedStep;
            UniformGridExperiment experiment = new UniformGridExperiment(n, intervalStart, currentEnd, referenceModel);

            double maxError = 0, maxRelativeError = 0;
            for (int i = 0; i < 1001; i++) {
                double target = intervalStart + (currentEnd - intervalStart) * i / 1000.0;
                double trueValue = referenceModel.evaluate(target);
                double error = Math.abs(trueValue - experiment.evaluateNewton(target));

                maxError = Math.max(maxError, error);
                maxRelativeError = Math.max(maxRelativeError, error / trueValue * 100);
            }

            xData[q] = n;
            yData[q] = maxError;
            System.out.println(String.format(Locale.US, "%3d | [%6.0f; %7.0f] | %10s | %s",
                    n, intervalStart, currentEnd, MathUtils.formatScientific(maxError), MathUtils.formatScientific(maxRelativeError)));
        }

        new Chart("Похибка при фіксованому кроці h=10000 (інтервал зростає)", "Число вузлів n", "max |f − P|, с")
                .line("max похибка", xData, yData, new Color(0x1f77b4), false)
                .points("", xData, yData, new Color(0x1f77b4))
                .save(outputDir + "/05_fixed_step.png");
    }

    private static void runNoiseInfluenceStudy(double intervalStart, double intervalEnd, double target, PowerModel referenceModel, String outputDir) throws Exception {
        System.out.println("\nДослідження 3: рівномірна сітка, значення у вузлах з похибкою вимірювання ±3% (Random(2026))");
        System.out.println(" n  | max|f-P| на [10000;160000] | max на краях (по 10%) | max у центрі | |f-P| у цільовій");

        int gridPoints = 1501;
        double[] referenceGridX = MathUtils.generateLinearSpace(intervalStart, intervalEnd, gridPoints);
        double[] referenceGridY = new double[gridPoints];
        for (int i = 0; i < gridPoints; i++) {
            referenceGridY[i] = referenceModel.evaluate(referenceGridX[i]);
        }

        Chart noisyFunctionChart = new Chart("Інтерполяція «вимірюваних» даних (шум ±3%)", "Розмір датасету", "Час, с");
        Chart noisyErrorChart = new Chart("Похибка |f(N) − P_n(N)| для даних із шумом", "Розмір датасету", "Похибка, с");
        noisyFunctionChart.line("f(N) — еталон", referenceGridX, referenceGridY, Color.BLACK, false);

        int[] nodeCounts = {5, 10, 20};
        for (int q = 0; q < nodeCounts.length; q++) {
            int n = nodeCounts[q];
            Random randomGenerator = new Random(2026);
            UniformGridExperiment baseExperiment = new UniformGridExperiment(n, intervalStart, intervalEnd, referenceModel);

            double[] noisyValues = new double[n];
            for (int i = 0; i < n; i++) {
                noisyValues[i] = baseExperiment.values[i] * (1 + 0.03 * (2 * randomGenerator.nextDouble() - 1));
            }

            double[] noisyNewtonCoefs = PolynomialMath.getNewtonCoefficients(baseExperiment.scaledNodes, noisyValues);
            double maxError = 0, edgeError = 0, centerError = 0;
            double[] approxNewton = new double[gridPoints];
            double[] newtonErrors = new double[gridPoints];

            for (int i = 0; i < gridPoints; i++) {
                approxNewton[i] = PolynomialMath.evaluateNewtonPolynomial(baseExperiment.scaledNodes, noisyNewtonCoefs, n - 1, referenceGridX[i] / 1000.0);
                newtonErrors[i] = Math.abs(referenceGridY[i] - approxNewton[i]);
                maxError = Math.max(maxError, newtonErrors[i]);

                double relativePosition = (referenceGridX[i] - intervalStart) / (intervalEnd - intervalStart);
                if (relativePosition < 0.1 || relativePosition > 0.9) edgeError = Math.max(edgeError, newtonErrors[i]);
                if (relativePosition > 0.4 && relativePosition < 0.6) centerError = Math.max(centerError, newtonErrors[i]);
            }

            double targetPrediction = PolynomialMath.evaluateNewtonPolynomial(baseExperiment.scaledNodes, noisyNewtonCoefs, n - 1, target / 1000.0);
            double targetError = Math.abs(referenceModel.evaluate(target) - targetPrediction);

            System.out.println(String.format(Locale.US, "%3d | %26s | %21s | %12s | %s",
                    n, MathUtils.formatScientific(maxError), MathUtils.formatScientific(edgeError),
                    MathUtils.formatScientific(centerError), MathUtils.formatScientific(targetError)));

            noisyFunctionChart.line("n = " + n, referenceGridX, approxNewton, Chart.PALETTE[q + 1], q > 0);
            noisyErrorChart.line("n = " + n, referenceGridX, newtonErrors, Chart.PALETTE[q + 1], false);
        }

        noisyFunctionChart.ylim(-200, 900).save(outputDir + "/06_noisy_polys.png");
        noisyErrorChart.save(outputDir + "/07_noisy_errors.png");
    }

    private static void runLagrangeComparison(double intervalStart, double intervalEnd, double target, double[] originalNodes, double[] originalValues, PowerModel referenceModel) {
        System.out.println("\nПорівняння з многочленом Лагранжа (n=10, 20; сітка 1501 точка)");
        int gridPoints = 1501;
        double[] gridX = MathUtils.generateLinearSpace(intervalStart, intervalEnd, gridPoints);

        for (int n : new int[]{5, 10, 20}) {
            UniformGridExperiment experiment = new UniformGridExperiment(n, intervalStart, intervalEnd, referenceModel);
            double maxDifference = 0;

            for (int i = 0; i < gridPoints; i++) {
                double newtonValue = experiment.evaluateNewton(gridX[i]);
                double lagrangeValue = PolynomialMath.evaluateLagrangePolynomial(experiment.scaledNodes, experiment.values, gridX[i] / 1000.0);
                maxDifference = Math.max(maxDifference, Math.abs(lagrangeValue - newtonValue));
            }
            System.out.println("  n=" + n + ": max|Лагранж − Ньютон| = " + MathUtils.formatScientific(maxDifference));
        }

        double originalNewtonCoefs[] = PolynomialMath.getNewtonCoefficients(originalNodes, originalValues);
        double newtonDataTarget = PolynomialMath.evaluateNewtonPolynomial(originalNodes, originalNewtonCoefs, originalNodes.length - 1, target);
        double lagrangeDataTarget = PolynomialMath.evaluateLagrangePolynomial(originalNodes, originalValues, target);

        System.out.println("  Дані (5 вузлів), N=" + target + ": Лагранж = " + MathUtils.format(lagrangeDataTarget, 4) + ", Ньютон = " + MathUtils.format(newtonDataTarget, 4));
    }
}
