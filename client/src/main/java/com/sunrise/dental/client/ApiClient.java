package com.sunrise.dental.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * The client's half of the web-service boundary.
 *
 * <p>This class is the reason the application counts as distributed rather than monolithic: it
 * holds no database connection, shares no classes with the server, and knows the clinic system
 * only through HTTP and JSON. Swapping the server implementation entirely would not require a
 * line of change here, as long as the published endpoints keep their shape.</p>
 *
 * <p>Credentials are sent as HTTP Basic on every request. A command-line process has nowhere
 * sensible to keep a session cookie between invocations, and the server's API filter chain is
 * built to verify credentials per request for exactly this reason.</p>
 */
public class ApiClient {

    private final HttpClient http;
    private final ObjectMapper json;
    private final String baseUrl;
    private final String authHeader;

    public ApiClient(String baseUrl, String username, String password) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (username + ":" + password).getBytes(StandardCharsets.UTF_8));
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.json = new ObjectMapper();
    }

    /** Verifies the credentials by making a request that requires authentication. */
    public boolean canAuthenticate() throws IOException, InterruptedException {
        return get("/api/appointments").statusCode() == 200;
    }

    public ApiResult listAppointments(String isoDate) throws IOException, InterruptedException {
        String path = isoDate == null || isoDate.isBlank()
                ? "/api/appointments"
                : "/api/appointments?date=" + isoDate;
        return toResult(get(path));
    }

    public ApiResult findAppointment(String appointmentNumber) throws IOException, InterruptedException {
        return toResult(get("/api/appointments/" + appointmentNumber));
    }

    public ApiResult registerAppointment(String requestBody) throws IOException, InterruptedException {
        return toResult(post("/api/appointments", requestBody));
    }

    public ApiResult previewBill(String appointmentNumber) throws IOException, InterruptedException {
        return toResult(get("/api/bills/preview/" + appointmentNumber));
    }

    public ApiResult issueBill(String appointmentNumber) throws IOException, InterruptedException {
        return toResult(post("/api/bills/" + appointmentNumber, ""));
    }

    public ApiResult markAttended(String appointmentNumber) throws IOException, InterruptedException {
        return toResult(post("/api/appointments/" + appointmentNumber + "/attended", ""));
    }

    public ApiResult cancelAppointment(String appointmentNumber) throws IOException, InterruptedException {
        return toResult(post("/api/appointments/" + appointmentNumber + "/cancel", ""));
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Turns an HTTP response into something the menu can act on.
     *
     * <p>The server distinguishes its failures by status code, and this is where that pays off:
     * a 409 means the slot is taken and the user should pick another, while a 500 means the
     * clinic system is broken and there is nothing the user can do. Collapsing both into
     * "request failed" would leave the receptionist unable to tell those apart.</p>
     */
    private ApiResult toResult(HttpResponse<String> response) {
        int status = response.statusCode();
        String body = response.body();

        if (status >= 200 && status < 300) {
            try {
                JsonNode node = body == null || body.isBlank() ? null : json.readTree(body);
                return ApiResult.success(status, node);
            } catch (IOException malformed) {
                return ApiResult.failure(status, "The server returned a response we could not read.");
            }
        }

        // Error bodies carry a "message" field, and validation failures add "fieldErrors".
        String message = "Request failed with status " + status + ".";
        try {
            JsonNode node = json.readTree(body);
            if (node.hasNonNull("message")) {
                message = node.get("message").asText();
            }
            if (node.has("fieldErrors")) {
                StringBuilder detail = new StringBuilder(message);
                node.get("fieldErrors").fields().forEachRemaining(entry ->
                        detail.append("\n    - ").append(entry.getKey())
                                .append(": ").append(entry.getValue().asText()));
                message = detail.toString();
            }
        } catch (Exception notJson) {
            if (status == 401) {
                message = "Your username or password was not accepted.";
            } else if (status == 403) {
                message = "Your account is not allowed to do that.";
            }
        }
        return ApiResult.failure(status, message);
    }
}
