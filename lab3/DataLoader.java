package lab3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class DataLoader {
    public static double[][] loadCsvData(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        int nodeCount = lines.size() - 1;
        double[] xValues = new double[nodeCount];
        double[] yValues = new double[nodeCount];

        for (int i = 0; i < nodeCount; i++) {
            String[] parts = lines.get(i + 1).split(",");
            xValues[i] = Double.parseDouble(parts[0]);
            yValues[i] = Double.parseDouble(parts[1]);
        }

        return new double[][]{xValues, yValues};
    }
}