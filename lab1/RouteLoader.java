package lab1;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouteLoader {
    public double[] latitudes;
    public double[] longitudes;
    public double[] elevations;
    public int nodeCount;

    public void loadFromCsv(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        nodeCount = lines.size() - 1;

        latitudes = new double[nodeCount];
        longitudes = new double[nodeCount];
        elevations = new double[nodeCount];

        for (int i = 0; i < nodeCount; i++) {
            String[] parts = lines.get(i + 1).split(",");
            latitudes[i] = Double.parseDouble(parts[0]);
            longitudes[i] = Double.parseDouble(parts[1]);
            elevations[i] = Double.parseDouble(parts[2]);
        }
    }

    public void loadFromApi() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        for (int i = 0; i < nodeCount; i++) {
            String coordinates = String.format(Locale.US, "%.6f,%.6f", latitudes[i], longitudes[i]);
            String apiUrl = "https://api.open-elevation.com/api/v1/lookup?locations=" + coordinates;

            HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(URI.create(apiUrl)).GET().build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            Matcher matcher = Pattern.compile("\"elevation\":\\s*([-\\d.]+)").matcher(response.body());

            if (matcher.find()) {
                elevations[i] = Double.parseDouble(matcher.group(1));
            } else {
                throw new IOException("API не повернув висоту для вузла " + i);
            }

            Thread.sleep(1000);
        }
    }
}
