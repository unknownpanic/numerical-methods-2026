package lab5;

import java.awt.Color;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import util.Chart;

public class Main {
    public static void main(String[] args) throws Exception {
        String outputDir = "lab5/output";
        new File(outputDir).mkdirs();

        double a = 0.0;
        double b = 24.0;
        double targetTolerance = 1e-12;

        System.out.println("Лабораторна робота №5 (Складова квадратурна формула Сімпсона. Методи підвищення точності. Адаптивний алгоритм)");

        double exactIntegral = runExactSolutionAndPlotLoad(a, b, outputDir);
        int nOpt = runSimpsonConvergenceAnalysis(a, b, exactIntegral, targetTolerance, outputDir);

        int n0 = selectMultipleOfEight(nOpt);
        runRefinementMethodsAnalysis(a, b, n0, exactIntegral);

        runAdaptiveAlgorithmAnalysis(a, b, exactIntegral, outputDir);
    }

    private static double runExactSolutionAndPlotLoad(double a, double b, String outputDir) throws Exception {
        double exactIntegral = ServerLoadModel.evaluateExactIntegral(a, b);

        System.out.println("\n1-2. Аналітична функція навантаження на сервер та точне значення інтегралу на [0; 24]");
        System.out.println("Функція: f(x) = 50 + 20 * sin(πx / 12) + 5 * e^(-0.2 * (x - 12)^2)");
        System.out.println("Точне значення інтегралу I_0 = " + MathUtils.format(exactIntegral, 12));

        double[] xGrid = MathUtils.generateLinearSpace(a, b, 400);
        double[] yGrid = new double[xGrid.length];
        for (int i = 0; i < xGrid.length; i++) {
            yGrid[i] = ServerLoadModel.evaluateLoad(xGrid[i]);
        }

        double[] nodesX = MathUtils.generateLinearSpace(a, b, 17);
        double[] nodesY = new double[nodesX.length];
        for (int i = 0; i < nodesX.length; i++) {
            nodesY[i] = ServerLoadModel.evaluateLoad(nodesX[i]);
        }

        new Chart("Навантаження на сервер f(x) = 50 + 20sin(πx/12) + 5e^(-0.2(x-12)^2)", "Час, x (год)", "Навантаження, f(x)")
                .line("f(x)", xGrid, yGrid, new Color(0x1f77b4), false)
                .points("Вузли (N=16)", nodesX, nodesY, new Color(0xd62728))
                .save(outputDir + "/01_server_load.png");

        return exactIntegral;
    }

    private static int runSimpsonConvergenceAnalysis(double a, double b, double exactIntegral, double targetTolerance, String outputDir) throws Exception {
        System.out.println("\n3-4. Дослідження залежності похибки ε(N) = |I(N) - I_0| для N = 10 ... 1000");
        System.out.println("   N  |        I(N)          |   ε(N) = |I(N) - I_0|");

        StringBuilder csvBuilder = new StringBuilder("N;I_N;eps_N\n");
        int nOpt = -1;
        double epsOpt = 0.0;

        int count = (1000 - 10) / 2 + 1;
        double[] nArray = new double[count];
        double[] logEpsArray = new double[count];

        int idx = 0;
        for (int n = 10; n <= 1000; n += 2) {
            double approx = SimpsonIntegrator.integrate(a, b, n);
            double err = SimpsonIntegrator.absoluteError(approx, exactIntegral);

            if (nOpt == -1 && err <= targetTolerance) {
                nOpt = n;
                epsOpt = err;
            }

            if (n <= 44 || n == 100 || n == 200 || n == 500 || n == 1000) {
                System.out.println(String.format(Locale.US, " %4d | %20.14f | %14.6e", n, approx, err));
            }
            csvBuilder.append(n).append(';').append(approx).append(';').append(err).append('\n');

            nArray[idx] = n;
            logEpsArray[idx] = Math.log10(Math.max(1e-16, err));
            idx++;
        }

        Files.writeString(Paths.get(outputDir, "simpson_convergence.csv"), csvBuilder.toString());

        System.out.println("Оптимальне число розбиттів (ε <= 1e-12): N_opt = " + nOpt
                + ", досягнута точність epsopt = " + MathUtils.formatScientific(epsOpt));

        new Chart("Залежність похибки складової формули Сімпсона ε(N) від N", "Число розбиттів N", "log10(ε(N))")
                .line("log10 ε(N)", nArray, logEpsArray, new Color(0x1f77b4), false)
                .points("N_opt = " + nOpt, new double[]{nOpt}, new double[]{Math.log10(Math.max(1e-16, epsOpt))}, new Color(0xd62728))
                .save(outputDir + "/02_error_vs_n.png");

        return nOpt;
    }

    private static int selectMultipleOfEight(int nOpt) {
        int raw = (int) Math.round((nOpt / 10.0) / 8.0) * 8;
        return Math.max(8, raw);
    }

    private static void runRefinementMethodsAnalysis(double a, double b, int n0, double exactIntegral) {
        System.out.println("\n5-8. Уточнення інтегралу методами Рунге-Ромберга та Ейткена (при N_0 кратному 8)");

        for (int candidateN0 : new int[]{n0, 16, 24}) {
            double iN0 = SimpsonIntegrator.integrate(a, b, candidateN0);
            double iN0Half = SimpsonIntegrator.integrate(a, b, candidateN0 / 2);
            double iN0Quarter = SimpsonIntegrator.integrate(a, b, candidateN0 / 4);

            double eps0 = SimpsonIntegrator.absoluteError(iN0, exactIntegral);
            double iR = SimpsonIntegrator.rungeRomberg(iN0, iN0Half);
            double epsR = SimpsonIntegrator.absoluteError(iR, exactIntegral);

            double iE = SimpsonIntegrator.aitkenExtrapolation(iN0, iN0Half, iN0Quarter);
            double epsE = SimpsonIntegrator.absoluteError(iE, exactIntegral);
            double p = SimpsonIntegrator.estimateAccuracyOrder(iN0, iN0Half, iN0Quarter);

            System.out.println("\n--- Розрахунок для N_0 = " + candidateN0 + " (N_0/2 = " + (candidateN0 / 2) + ", N_0/4 = " + (candidateN0 / 4) + ") ---");
            System.out.println(String.format(Locale.US, "Складова формула Сімпсона: I(N_0)   = %.14f, eps0 = %.6e", iN0, eps0));
            System.out.println(String.format(Locale.US, "Складова формула Сімпсона: I(N_0/2) = %.14f, eps  = %.6e", iN0Half, Math.abs(iN0Half - exactIntegral)));
            System.out.println(String.format(Locale.US, "Складова формула Сімпсона: I(N_0/4) = %.14f, eps  = %.6e", iN0Quarter, Math.abs(iN0Quarter - exactIntegral)));
            System.out.println(String.format(Locale.US, "Метод Рунге-Ромберга:      I_R      = %.14f, epsR = %.6e", iR, epsR));
            System.out.println(String.format(Locale.US, "Метод Ейткена (p=%.4f):  I_E      = %.14f, epsE = %.6e", p, iE, epsE));
        }
    }

    private static void runAdaptiveAlgorithmAnalysis(double a, double b, double exactIntegral, String outputDir) throws Exception {
        System.out.println("\n9. Дослідження адаптивного алгоритму Сімпсона залежно від параметра δ");
        System.out.println("    δ     |       I_adap         |  ε = |I_adap - I_0| | Число обчислень f(x)");

        int minExp = -12;
        int maxExp = -1;
        int count = maxExp - minExp + 1;
        double[] logDeltaArray = new double[count];
        double[] logEpsArray = new double[count];
        double[] evalsArray = new double[count];

        int idx = 0;
        for (int exp = maxExp; exp >= minExp; exp--) {
            double delta = Math.pow(10.0, exp);
            AdaptiveIntegrator.AdaptiveResult res = AdaptiveIntegrator.integrate(a, b, delta, exactIntegral);

            System.out.println(String.format(Locale.US, " %8.1e | %20.14f | %19.6e | %8d",
                    delta, res.integralValue, res.absoluteError, res.functionEvaluations));

            int plotIdx = exp - minExp;
            logDeltaArray[plotIdx] = exp;
            logEpsArray[plotIdx] = Math.log10(Math.max(1e-16, res.absoluteError));
            evalsArray[plotIdx] = res.functionEvaluations;
            idx++;
        }

        new Chart("Точність адаптивного алгоритму залежно від параметра δ", "log10(δ)", "log10(ε)")
                .line("log10 ε(δ)", logDeltaArray, logEpsArray, new Color(0x2ca02c), false)
                .points("Експерименти", logDeltaArray, logEpsArray, new Color(0xd62728))
                .save(outputDir + "/03_adaptive_error_vs_delta.png");

        new Chart("Кількість обчислень функції f(x) в адаптивному алгоритмі", "log10(δ)", "Кількість викликів f(x)")
                .line("Виклики f(x)", logDeltaArray, evalsArray, new Color(0xff7f0e), false)
                .points("Вузли", logDeltaArray, evalsArray, new Color(0x1f77b4))
                .save(outputDir + "/04_adaptive_evals_vs_delta.png");
    }
}
