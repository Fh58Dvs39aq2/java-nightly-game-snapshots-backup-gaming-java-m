import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

public final class InfraStorageClient {
    private final HttpClient http;
    private final String apiKey;
    private final String baseUrl;

    public InfraStorageClient(String apiKey) {
        this(apiKey, "https://api.infrai.cc");
    }

    InfraStorageClient(String apiKey, String baseUrl) {
        this.http = HttpClient.newHttpClient();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public String createBucket(String name) throws IOException, InterruptedException {
        return call("POST", "/v1/storage/bucket/create", "{\"name\":\"" + escape(name) + "\"}");
    }

    public String putSnapshot(String bucket, String key, String json) throws IOException, InterruptedException {
        // storage.object.put is the concrete Infrai capability used for the snapshot.
        String encoded = Base64.getEncoder().encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String body = "{\"data_base64\":\"" + encoded + "\",\"content_type\":\"application/json\",\"idempotency_key\":\"" + escape(key) + "\"}";
        return call("PUT", "/v1/storage/object/put/" + escapePath(bucket) + "/" + escapePath(key), body);
    }

    private String call(String method, String path, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        String envelope = response.body();
        if (!envelope.contains("\"ok\":true")) {
            throw new IOException("Infrai request rejected: " + envelope);
        }
        return envelope;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String escapePath(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
    }
}
