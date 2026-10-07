package lab1;

import java.awt.Color;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;
import util.Chart;

public class Main {
    public static void main(String[] args) throws Exception {
        String outputDir = "lab1/output";
        new File(outputDir).mkdirs();

        RouteLoader route = loadDataViaMenu();

        int nodeCount = route.nodeCount;
        System.out.println("Кількість вузлів: " + nodeCount);

        double[] distances = calculateDistances(route);
        saveAndPrintTabulation(route, distances, outputDir);

        new Chart("Профіль висоти маршруту (вузли GPS)", "Кумулятивна відстань, м", "Висота, м")
                .line("Ламана за вузлами", distances, route.elevations, new Color(0x2ca02c), false)
                .points("Вузли", distances, route.elevations, new Color(0xd62728))
                .save(outputDir + "/01_nodes.png");

        CubicSpline fullSpline = new CubicSpline(distances, route.elevations);
        printSplineCoefficients(fullSpline, outputDir);

        evaluateSplineErrorsAndSubsets(fullSpline, distances, route.elevations, outputDir);
        analyzeRouteCharacteristics(route.elevations, distances, fullSpline, outputDir);
    }

    private static RouteLoader loadDataViaMenu() throws Exception {
        RouteLoader route = new RouteLoader();
        route.loadFromCsv("lab1/resource/route.csv");

        Scanner scanner = new Scanner(System.in);
        System.out.println("Оберіть джерело висот для маршруту:");
        System.out.println("1 - Прочитати локально (з файлу lab1/resource/route.csv)");
        System.out.println("2 - Завантажити через Open-Elevation API");
        System.out.print("Ваш вибір (1 або 2): ");

        String choice = scanner.nextLine().trim();

        if (choice.equals("2")) {
            try {
                System.out.println("\nСпроба завантаження даних з Open-Elevation API. Зачекайте будь ласка...");
                route.loadFromApi();
                System.out.println("\nВисоти успішно отримано з Open-Elevation API.");
            } catch (Exception e) {
                System.out.println("\nAPI недоступне (" + e + "). Використано дані з route.csv.");
            }
        } else {
            System.out.println("\nДані прочитано з локального файлу route.csv.");
        }

        return route;
    }

    private static double[] calculateDistances(RouteLoader route) {
        double[] distances = new double[route.nodeCount];
        for (int i = 1; i < route.nodeCount; i++) {
            distances[i] = distances[i - 1] + MathUtils.calculateDistance(
                    route.latitudes[i - 1], route.longitudes[i - 1],
                    route.latitudes[i], route.longitudes[i]
            );
        }
        return distances;
    }

    private static void saveAndPrintTabulation(RouteLoader route, double[] distances, String outputDir) throws Exception {
        StringBuilder tabulationOutput = new StringBuilder("№ | Latitude | Longitude | Elevation (m) | Distance (m)\n");
        System.out.println("\nТабуляція вузлів:");
        System.out.println(" № | Latitude  | Longitude | Elevation (m) | Distance (m)");

        for (int i = 0; i < route.nodeCount; i++) {
            String tableRow = String.format(Locale.US, "%2d | %.6f | %.6f | %8.2f      | %9.2f",
                    i, route.latitudes[i], route.longitudes[i], route.elevations[i], distances[i]);
            System.out.println(tableRow);
            tabulationOutput.append(tableRow).append('\n');
        }
        Files.writeString(Paths.get(outputDir, "tabulation.txt"), tabulationOutput.toString());
    }

    private static void printSplineCoefficients(CubicSpline spline, String outputDir) throws Exception {
        System.out.println("\nКоефіцієнти системи (A, B, C, D) для c_1..c_" + (spline.nodes.length - 2) + ":");
        System.out.println(" i |        A |        B |        C |        D");
        for (int k = 0; k < spline.sysMain.length; k++) {
            System.out.println(String.format(Locale.US, "%2d | %8.3f | %8.3f | %8.3f | %10.5f",
                    k + 1, spline.sysLower[k], spline.sysMain[k], spline.sysUpper[k], spline.sysRhs[k]));
        }

        System.out.println("\nРозв'язок методом прогонки c_i:");
        for (int k = 0; k < spline.innerCoefC.length; k++) {
            System.out.println(String.format(Locale.US, "c[%2d] = % .8f", k + 1, spline.innerCoefC[k]));
        }
        System.out.println("Перевірка: max|Ac - delta| = " + String.format(Locale.US, "%.3e", spline.maxResidual));

        StringBuilder coefficientsOutput = new StringBuilder();
        System.out.println("\nКоефіцієнти кубічних сплайнів S_i(x)=a+b(x-x_i)+c(x-x_i)^2+d(x-x_i)^3:");
        String headerRow = " i |  x_i, м   |    a_i  |    b_i   |     c_i     |      d_i";
        System.out.println(headerRow);
        coefficientsOutput.append(headerRow).append('\n');

        for (int i = 0; i < spline.nodes.length - 1; i++) {
            String tableRow = String.format(Locale.US, "%2d | %9.2f | %7.1f | % .6f | % .3e | % .3e",
                    i, spline.nodes[i], spline.coefA[i], spline.coefB[i], spline.coefC[i], spline.coefD[i]);
            System.out.println(tableRow);
            coefficientsOutput.append(tableRow).append('\n');
        }
        Files.writeString(Paths.get(outputDir, "spline_coefficients.txt"), coefficientsOutput.toString());
    }

    private static void evaluateSplineErrorsAndSubsets(CubicSpline fullSpline, double[] distances, double[] elevations, String outputDir) throws Exception {
        int nodeCount = distances.length;
        double maxNodeError = 0;

        for (int i = 0; i < nodeCount; i++) {
            maxNodeError = Math.max(maxNodeError, Math.abs(fullSpline.calculateValue(distances[i]) - elevations[i]));
        }
        System.out.println("\nМаксимальна похибка у вузлах (21 вузол): " + String.format(Locale.US, "%.3e", maxNodeError));

        int gridSize = 500;
        double[] denseDistances = MathUtils.generateLinearSpace(distances[0], distances[nodeCount - 1], gridSize);
        double[] denseElevations = new double[gridSize];

        for (int i = 0; i < gridSize; i++) {
            denseElevations[i] = fullSpline.calculateValue(denseDistances[i]);
        }

        new Chart("Кубічний сплайн по всіх вузлах", "Кумулятивна відстань, м", "Висота, м")
                .line("S(x), " + nodeCount + " вузлів", denseDistances, denseElevations, new Color(0x1f77b4), false)
                .points("Вузли", distances, elevations, new Color(0xd62728))
                .save(outputDir + "/02_spline_full.png");

        int[] nodeCountsToTest = {10, 15, 20};
        Chart comparisonChart = new Chart("Вплив кількості вузлів на сплайн", "Кумулятивна відстань, м", "Висота, м");
        Chart errorChart = new Chart("Похибка |S_k(x) − S_21(x)|", "Кумулятивна відстань, м", "Похибка, м");
        comparisonChart.line("S_21 (еталон)", denseDistances, denseElevations, Color.BLACK, false);

        System.out.println("\nВплив кількості вузлів (еталон — сплайн по всіх " + nodeCount + " вузлах):");
        System.out.println(" k  | max|S_k-S_21|, м | середня, м | max у вихідних точках, м | RMS у вихідних точках, м");

        StringBuilder errorTableOutput = new StringBuilder("k;max_vs_ref;mean_vs_ref;max_at_data;rms_at_data\n");

        for (int q = 0; q < nodeCountsToTest.length; q++) {
            int k = nodeCountsToTest[q];
            int[] currentIndices = MathUtils.getUniformIndices(nodeCount, k);
            CubicSpline partialSpline = new CubicSpline(
                    MathUtils.createSubset(distances, currentIndices),
                    MathUtils.createSubset(elevations, currentIndices)
            );

            double[] approxElevations = new double[gridSize];
            double[] gridErrors = new double[gridSize];
            double maxGridError = 0;
            double meanGridError = 0;
            double maxErrorPosition = 0;

            for (int i = 0; i < gridSize; i++) {
                approxElevations[i] = partialSpline.calculateValue(denseDistances[i]);
                gridErrors[i] = Math.abs(approxElevations[i] - denseElevations[i]);

                if (gridErrors[i] > maxGridError) {
                    maxGridError = gridErrors[i];
                    maxErrorPosition = denseDistances[i];
                }
                meanGridError += gridErrors[i] / gridSize;
            }

            System.out.println("  (k=" + k + ": максимум відхилення в x = " + MathUtils.format(maxErrorPosition, 1) +
                    " м; пропущені вузли: " + MathUtils.getSkippedIndicesInfo(nodeCount, currentIndices) + ")");

            double maxDataError = 0;
            double rmsDataError = 0;

            for (int i = 0; i < nodeCount; i++) {
                double error = Math.abs(partialSpline.calculateValue(distances[i]) - elevations[i]);
                maxDataError = Math.max(maxDataError, error);
                rmsDataError += error * error / nodeCount;
            }
            rmsDataError = Math.sqrt(rmsDataError);

            System.out.println(String.format(Locale.US, "%3d | %16.3f | %11.3f | %24.3f | %.3f",
                    k, maxGridError, meanGridError, maxDataError, rmsDataError));

            errorTableOutput.append(String.format(Locale.US, "%d;%.4f;%.4f;%.4f;%.4f%n",
                    k, maxGridError, meanGridError, maxDataError, rmsDataError));

            comparisonChart.line(k + " вузлів", denseDistances, approxElevations, Chart.PALETTE[q + 2], q > 0);
            errorChart.line(k + " вузлів", denseDistances, gridErrors, Chart.PALETTE[q + 2], false);

            new Chart("S(x) на " + k + " вузлах та вихідні дані", "Кумулятивна відстань, м", "Висота, м")
                    .line("S_" + k + "(x)", denseDistances, approxElevations, new Color(0x1f77b4), false)
                    .points("Усі 21 вихідні точки", distances, elevations, new Color(0xd62728))
                    .points("Вузли сплайна", MathUtils.createSubset(distances, currentIndices), MathUtils.createSubset(elevations, currentIndices), Color.BLACK)
                    .save(outputDir + "/03_spline_" + k + ".png");
        }

        Files.writeString(Paths.get(outputDir, "nodes_error.csv"), errorTableOutput.toString());
        comparisonChart.points("Вихідні точки", distances, elevations, new Color(0xd62728)).save(outputDir + "/04_nodes_compare.png");
        errorChart.save(outputDir + "/05_nodes_error.png");

        evaluateUnusedNodes(distances, elevations, nodeCount);
    }

    private static void evaluateUnusedNodes(double[] distances, double[] elevations, int nodeCount) {
        int[] tenNodesIndices = MathUtils.getUniformIndices(nodeCount, 10);
        CubicSpline tenNodesSpline = new CubicSpline(
                MathUtils.createSubset(distances, tenNodesIndices),
                MathUtils.createSubset(elevations, tenNodesIndices)
        );

        Set<Integer> usedInTenNodes = new HashSet<>();
        for (int v : tenNodesIndices) {
            usedInTenNodes.add(v);
        }

        System.out.println("\nПохибка сплайна з 10 вузлів у вузлах, що не використовувались:");
        for (int i = 0; i < nodeCount; i++) {
            if (!usedInTenNodes.contains(i)) {
                double predicted = tenNodesSpline.calculateValue(distances[i]);
                System.out.println(String.format(Locale.US, "  вузол %2d: x=%8.2f  y=%7.1f  S=%9.2f  |err|=%6.2f",
                        i, distances[i], elevations[i], predicted, Math.abs(predicted - elevations[i])));
            }
        }
    }

    private static void analyzeRouteCharacteristics(double[] elevations, double[] distances, CubicSpline fullSpline, String outputDir) throws Exception {
        int nodeCount = elevations.length;
        System.out.println("\nХарактеристики маршруту");

        double totalAscent = 0;
        double totalDescent = 0;

        for (int i = 1; i < nodeCount; i++) {
            totalAscent += Math.max(elevations[i] - elevations[i - 1], 0);
            totalDescent += Math.max(elevations[i - 1] - elevations[i], 0);
        }

        System.out.println("Загальна довжина маршруту (м): " + MathUtils.format(distances[nodeCount - 1], 2));
        System.out.println("Сумарний набір висоти (м): " + MathUtils.format(totalAscent, 1));
        System.out.println("Сумарний спуск (м): " + MathUtils.format(totalDescent, 1));
        System.out.println("Різниця висот старт-фініш (м): " + MathUtils.format(elevations[nodeCount - 1] - elevations[0], 1));

        int gridSize = 500;
        double[] denseDistances = MathUtils.generateLinearSpace(distances[0], distances[nodeCount - 1], gridSize);
        double[] gradients = new double[gridSize];
        double maxGradient = -1e9;
        double minGradient = 1e9;
        double meanAbsGradient = 0;
        double steepLength = 0;
        double gridStep = denseDistances[1] - denseDistances[0];

        for (int i = 0; i < gridSize; i++) {
            gradients[i] = fullSpline.calculateDerivative(denseDistances[i]) * 100;
            maxGradient = Math.max(maxGradient, gradients[i]);
            minGradient = Math.min(minGradient, gradients[i]);
            meanAbsGradient += Math.abs(gradients[i]) / gridSize;

            if (Math.abs(gradients[i]) > 15) {
                steepLength += gridStep;
            }
        }

        System.out.println("\nАналіз градієнта (за похідною сплайна)");
        System.out.println("Максимальний підйом (%): " + MathUtils.format(maxGradient, 2));
        System.out.println("Максимальний спуск (%): " + MathUtils.format(minGradient, 2));
        System.out.println("Середній градієнт |S'| (%): " + MathUtils.format(meanAbsGradient, 2));
        System.out.println("Середній градієнт по маршруту (перепад/довжина, %): " + MathUtils.format((elevations[nodeCount - 1] - elevations[0]) / distances[nodeCount - 1] * 100, 2));
        System.out.println("Довжина ділянок з крутизною > 15% (м): " + MathUtils.format(steepLength, 1) + " (" + MathUtils.format(steepLength / distances[nodeCount - 1] * 100, 1) + "% маршруту)");

        new Chart("Градієнт профілю (похідна сплайна)", "Кумулятивна відстань, м", "Градієнт, %")
                .line("S'(x)·100%", denseDistances, gradients, new Color(0xd62728), false)
                .line("+15%", new double[]{denseDistances[0], denseDistances[gridSize - 1]}, new double[]{15, 15}, Color.GRAY, true)
                .line("−15%", new double[]{denseDistances[0], denseDistances[gridSize - 1]}, new double[]{-15, -15}, Color.GRAY, true)
                .save(outputDir + "/06_gradient.png");

        double bodyMass = 80;
        double gravity = 9.81;
        double mechanicalEnergy = bodyMass * gravity * totalAscent;

        System.out.println("\nМеханічна робота (маса 80 кг)");
        System.out.println("Механічна робота (Дж): " + MathUtils.format(mechanicalEnergy, 1));
        System.out.println("Механічна робота (кДж): " + MathUtils.format(mechanicalEnergy / 1000, 3));
        System.out.println("Енергія (ккал): " + MathUtils.format(mechanicalEnergy / 4184, 2));
    }
}