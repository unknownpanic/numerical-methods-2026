package lab4;

import java.awt.Color;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import util.Chart;

public class Main {
    public static void main(String[] args) throws Exception {
        String outputDir = "lab4/output";
        new File(outputDir).mkdirs();

        double x0 = 1.0;
        double baseStep = 1e-3;

        System.out.println("Лабораторна робота №4 (Чисельне диференціювання. Методи Рунге-Ромберга та Ейткена)");

        double exactDerivative = runAnalyticalSolutionAndPlotModel(x0, outputDir);
        double[] optimalStepResult = runStepDependenceAnalysis(x0, exactDerivative, outputDir);
        double h0 = optimalStepResult[0];
        double r0 = optimalStepResult[1];

        double[] twoStepValues = runTwoStepCalculations(x0, baseStep, exactDerivative);
        double yH = twoStepValues[0];
        double y2H = twoStepValues[1];
        double r1 = twoStepValues[2];

        double[] rungeRombergResult = runRungeRombergMethod(yH, y2H, exactDerivative, r1);
        double yR = rungeRombergResult[0];
        double r2 = rungeRombergResult[1];

        double[] aitkenResult = runAitkenMethod(x0, baseStep, yH, y2H, exactDerivative, r1);
        double yE = aitkenResult[0];
        double r3 = aitkenResult[1];
        double estimatedOrder = aitkenResult[2];

        printSummaryAndPlotComparison(x0, baseStep, exactDerivative, h0, r0, yH, r1, yR, r2, yE, r3, estimatedOrder, outputDir);
    }

    private static double runAnalyticalSolutionAndPlotModel(double x0, String outputDir) throws Exception {
        double moistureAtX0 = SoilMoistureModel.evaluateMoisture(x0);
        double exactDerivative = SoilMoistureModel.evaluateExactDerivative(x0);

        System.out.println("\n1. Аналітичне розв'язання в точці x_0 = " + MathUtils.format(x0, 1));
        System.out.println("Функція вологості ґрунту: M(t) = 50 * e^(-0.1t) + 5 * sin(t)");
        System.out.println("Перша похідна:            M'(t) = -5 * e^(-0.1t) + 5 * cos(t)");
        System.out.println("M(1.0)  = " + MathUtils.format(moistureAtX0, 6));
        System.out.println("y'(x_0) = M'(1.0) = " + MathUtils.format(exactDerivative, 12) + " (округлено: " + MathUtils.format(exactDerivative, 6) + ")");

        double[] tGrid = MathUtils.generateLinearSpace(0.0, 20.0, 400);
        double[] mGrid = new double[tGrid.length];
        double[] dmGrid = new double[tGrid.length];

        for (int i = 0; i < tGrid.length; i++) {
            mGrid[i] = SoilMoistureModel.evaluateMoisture(tGrid[i]);
            dmGrid[i] = SoilMoistureModel.evaluateExactDerivative(tGrid[i]);
        }

        new Chart("Модель вологості ґрунту M(t) = 50e^(-0.1t) + 5sin(t)", "Час t", "Вологість M(t)")
                .line("M(t)", tGrid, mGrid, new Color(0x1f77b4), false)
                .points("t_0 = 1", new double[]{x0}, new double[]{moistureAtX0}, new Color(0xd62728))
                .save(outputDir + "/01_moisture_model.png");

        new Chart("Точна перша похідна M'(t) = -5e^(-0.1t) + 5cos(t)", "Час t", "Швидкість зміни M'(t)")
                .line("M'(t)", tGrid, dmGrid, new Color(0x2ca02c), false)
                .points("M'(1) = -1.822676", new double[]{x0}, new double[]{exactDerivative}, new Color(0xd62728))
                .save(outputDir + "/02_exact_derivative.png");

        return exactDerivative;
    }

    private static double[] runStepDependenceAnalysis(double x0, double exactDerivative, String outputDir) throws Exception {
        System.out.println("\n2. Дослідження залежності похибки R(h) = |y'_0(h) - y'(x_0)| від кроку h (10^-20 ... 10^3)");
        System.out.println("    h     |     y'_0(h)        |   R = |y'_0(h) - y'(x_0)|");

        StringBuilder csvBuilder = new StringBuilder("h;y0_prime;R\n");
        double bestDecadeH = 1.0;
        double bestDecadeError = Double.MAX_VALUE;

        for (int exp = -20; exp <= 3; exp++) {
            double h = Math.pow(10.0, exp);
            double approx = NumericalDifferentiator.centralDifference(x0, h);
            double error = NumericalDifferentiator.absoluteError(approx, exactDerivative);

            if (error < bestDecadeError) {
                bestDecadeError = error;
                bestDecadeH = h;
            }

            if (exp >= -16 && exp <= 1) {
                System.out.println(String.format(Locale.US, " %8.1e | %18.12f | %14.6e", h, approx, error));
            } else {
                System.out.println(String.format(Locale.US, " %8.1e | %18.6e | %14.6e", h, approx, error));
            }
            csvBuilder.append(h).append(';').append(approx).append(';').append(error).append('\n');
        }

        double bestFineH = bestDecadeH;
        double bestFineError = bestDecadeError;
        for (int exp = -20; exp <= 2; exp++) {
            for (int mantissa = 1; mantissa <= 9; mantissa++) {
                double h = mantissa * Math.pow(10.0, exp);
                double approx = NumericalDifferentiator.centralDifference(x0, h);
                double error = NumericalDifferentiator.absoluteError(approx, exactDerivative);
                if (error < bestFineError) {
                    bestFineError = error;
                    bestFineH = h;
                }
            }
        }

        Files.writeString(Paths.get(outputDir, "step_errors.csv"), csvBuilder.toString());

        System.out.println("Оптимальний крок серед степенів 10^k: h_0 = " + MathUtils.formatStepCompact(bestDecadeH)
                + ", досягнута точність R_0 = " + String.format(Locale.US, "%.6e", bestDecadeError));
        System.out.println("Оптимальний крок у форматі m*10^k:    h_0 = " + MathUtils.formatStepCompact(bestFineH)
                + ", досягнута точність R_0 = " + String.format(Locale.US, "%.6e", bestFineError));

        double[] hPlotGrid = MathUtils.generateLogSpace(-16, 1, 10);
        double[] logH = new double[hPlotGrid.length];
        double[] logR = new double[hPlotGrid.length];

        for (int i = 0; i < hPlotGrid.length; i++) {
            double approx = NumericalDifferentiator.centralDifference(x0, hPlotGrid[i]);
            double err = Math.max(1e-15, NumericalDifferentiator.absoluteError(approx, exactDerivative));
            logH[i] = Math.log10(hPlotGrid[i]);
            logR[i] = Math.log10(err);
        }

        new Chart("Залежність похибки R(h) від кроку h (лог. масштаб)", "log10(h)", "log10(R)")
                .line("log10 R(h)", logH, logR, new Color(0x1f77b4), false)
                .points("h_0 = " + MathUtils.formatStepCompact(bestFineH),
                        new double[]{Math.log10(bestFineH)},
                        new double[]{Math.log10(bestFineError)},
                        new Color(0xd62728))
                .save(outputDir + "/03_error_vs_step.png");

        return new double[]{bestFineH, bestFineError};
    }

    private static double[] runTwoStepCalculations(double x0, double h, double exactDerivative) {
        double yH = NumericalDifferentiator.centralDifference(x0, h);
        double y2H = NumericalDifferentiator.centralDifference(x0, 2.0 * h);
        double r1 = NumericalDifferentiator.absoluteError(yH, exactDerivative);
        double r2H = NumericalDifferentiator.absoluteError(y2H, exactDerivative);

        System.out.println("\n3-5. Обчислення похідної та похибки для кроків h = 10^-3 та 2h = 2*10^-3");
        System.out.println(String.format(Locale.US, "При h  = %.4f: y'_0(h)  = %.12f, похибка R_1   = %.6e", h, yH, r1));
        System.out.println(String.format(Locale.US, "При 2h = %.4f: y'_0(2h) = %.12f, похибка R(2h) = %.6e", 2.0 * h, y2H, r2H));

        return new double[]{yH, y2H, r1};
    }

    private static double[] runRungeRombergMethod(double yH, double y2H, double exactDerivative, double r1) {
        double yR = NumericalDifferentiator.rungeRomberg(yH, y2H);
        double r2 = NumericalDifferentiator.absoluteError(yR, exactDerivative);

        System.out.println("\n6. Уточнення значення похідної методом Рунге-Ромберга");
        System.out.println(String.format(Locale.US, "Уточнене значення: y'_R = %.12f", yR));
        System.out.println(String.format(Locale.US, "Похибка методу:    R_2  = |y'_R - y'(x_0)| = %.6e", r2));
        System.out.println(String.format(Locale.US, "Зменшення похибки: R_1 / R_2 = %.1f разів", r1 / r2));

        return new double[]{yR, r2};
    }

    private static double[] runAitkenMethod(double x0, double h, double yH, double y2H, double exactDerivative, double r1) {
        double y4H = NumericalDifferentiator.centralDifference(x0, 4.0 * h);
        double r4H = NumericalDifferentiator.absoluteError(y4H, exactDerivative);
        double yE = NumericalDifferentiator.aitkenExtrapolation(yH, y2H, y4H);
        double p = NumericalDifferentiator.estimateAccuracyOrder(yH, y2H, y4H);
        double r3 = NumericalDifferentiator.absoluteError(yE, exactDerivative);

        System.out.println("\n7. Уточнення значення похідної та оцінка порядку точності методом Ейткена");
        System.out.println(String.format(Locale.US, "При 4h = %.4f:   y'_0(4h) = %.12f, похибка R(4h) = %.6e", 4.0 * h, y4H, r4H));
        System.out.println(String.format(Locale.US, "Уточнене значення: y'_E     = %.12f", yE));
        System.out.println(String.format(Locale.US, "Оцінка порядку:    p        = %.6f (теоретичний p = 2)", p));
        System.out.println(String.format(Locale.US, "Похибка методу:    R_3      = |y'_E - y'(x_0)| = %.6e", r3));
        System.out.println(String.format(Locale.US, "Зменшення похибки: R_1 / R_3 = %.1f разів", r1 / r3));

        return new double[]{yE, r3, p};
    }

    private static void printSummaryAndPlotComparison(double x0, double baseStep, double exactDerivative,
                                                      double h0, double r0, double yH, double r1,
                                                      double yR, double r2, double yE, double r3,
                                                      double estimatedOrder, String outputDir) throws Exception {
        System.out.println("\n8. Підсумкова таблиця результатів (x_0 = 1.0, базовий крок h = 10^-3)");
        System.out.println(" Метод                       |   Значення похідної  | Абсолютна похибка R");
        System.out.println(String.format(Locale.US, " Точне значення M'(1)        | %20.12f | %14.6e", exactDerivative, 0.0));
        System.out.println(String.format(Locale.US, " Оптимальний крок (h_0=%s) | %20.12f | %14.6e",
                MathUtils.formatStepCompact(h0), NumericalDifferentiator.centralDifference(x0, h0), r0));
        System.out.println(String.format(Locale.US, " Центральна різниця (h=1e-3) | %20.12f | %14.6e", yH, r1));
        System.out.println(String.format(Locale.US, " Метод Рунге-Ромберга        | %20.12f | %14.6e", yR, r2));
        System.out.println(String.format(Locale.US, " Метод Ейткена (p=%.4f)    | %20.12f | %14.6e", estimatedOrder, yE, r3));

        double[] stepsGrid = MathUtils.generateLogSpace(-4, -1, 15);
        double[] logSteps = new double[stepsGrid.length];
        double[] logCentralErr = new double[stepsGrid.length];
        double[] logRRErr = new double[stepsGrid.length];
        double[] logAitkenErr = new double[stepsGrid.length];

        for (int i = 0; i < stepsGrid.length; i++) {
            double s = stepsGrid[i];
            double d1 = NumericalDifferentiator.centralDifference(x0, s);
            double d2 = NumericalDifferentiator.centralDifference(x0, 2.0 * s);
            double d4 = NumericalDifferentiator.centralDifference(x0, 4.0 * s);
            double rr = NumericalDifferentiator.rungeRomberg(d1, d2);
            double ait = NumericalDifferentiator.aitkenExtrapolation(d1, d2, d4);

            logSteps[i] = Math.log10(s);
            logCentralErr[i] = Math.log10(Math.max(1e-16, NumericalDifferentiator.absoluteError(d1, exactDerivative)));
            logRRErr[i] = Math.log10(Math.max(1e-16, NumericalDifferentiator.absoluteError(rr, exactDerivative)));
            logAitkenErr[i] = Math.log10(Math.max(1e-16, NumericalDifferentiator.absoluteError(ait, exactDerivative)));
        }

        new Chart("Порівняння похибок методів диференціювання", "log10(h)", "log10(R)")
                .line("Центральна різниця R_1", logSteps, logCentralErr, new Color(0x1f77b4), false)
                .line("Метод Ейткена R_3", logSteps, logAitkenErr, new Color(0xff7f0e), true)
                .line("Метод Рунге-Ромберга R_2", logSteps, logRRErr, new Color(0x2ca02c), false)
                .points("h = 10^-3", new double[]{-3.0, -3.0, -3.0},
                        new double[]{Math.log10(r1), Math.log10(r3), Math.log10(r2)}, new Color(0xd62728))
                .save(outputDir + "/04_methods_comparison.png");
    }
}
