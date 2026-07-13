package kroviq.qmetry;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.UUID;

public class QMetryApiClient {

    private final String apiKey;
    private final String authHeader;
    private static final int TIMEOUT_MS = 30000;

    public QMetryApiClient(QMetryConfig config) {
        this.apiKey = config.getApiKey();

        String user = config.getUsername();
        String pass = config.getPassword();
        if (!user.isEmpty() && !pass.isEmpty()) {
            this.authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
        } else {
            this.authHeader = null;
        }
    }

    public String postJson(String url, String jsonBody) throws IOException {
        HttpURLConnection conn = openConnection(url, "POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        return readResponse(conn);
    }

    public String postFile(String url, File file) throws IOException {
        String boundary = "----Kroviq" + UUID.randomUUID().toString().replace("-", "");

        HttpURLConnection conn = openConnection(url, "POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n"
                + "Content-Type: application/json\r\n\r\n";
            os.write(header.getBytes(StandardCharsets.UTF_8));

            Files.copy(file.toPath(), os);

            String footer = "\r\n--" + boundary + "--\r\n";
            os.write(footer.getBytes(StandardCharsets.UTF_8));
        }

        return readResponse(conn);
    }

    public String get(String url) throws IOException {
        HttpURLConnection conn = openConnection(url, "GET");
        return readResponse(conn);
    }

    private HttpURLConnection openConnection(String url, String method) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);

        if (apiKey != null && !apiKey.isEmpty()) {
            conn.setRequestProperty("apiKey", apiKey);
        }
        if (authHeader != null) {
            conn.setRequestProperty("Authorization", authHeader);
        }

        return conn;
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        int status = conn.getResponseCode();
        InputStream stream = (status >= 200 && status < 300)
            ? conn.getInputStream()
            : conn.getErrorStream();

        if (stream == null) {
            throw new IOException("HTTP " + status + " -- no response body");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String body = sb.toString();

            if (status >= 200 && status < 300) {
                return body;
            }
            // Extract clean message from HTML error pages
            String message = body;
            if (body.contains("<title>")) {
                int start = body.indexOf("<title>") + 7;
                int end = body.indexOf("</title>", start);
                if (end > start) {
                    message = body.substring(start, end).trim();
                }
            }
            throw new IOException("HTTP " + status + ": " + message);
        } finally {
            conn.disconnect();
        }
    }
}
