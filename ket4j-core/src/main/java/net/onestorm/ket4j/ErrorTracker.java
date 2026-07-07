package net.onestorm.ket4j;

import net.onestorm.ket4j.sanitizer.Sanitizer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

public class ErrorTracker {

    private static final Logger LOGGER = Logger.getLogger(ErrorTracker.class.getName());

    private static final int MAX_ENVIRONMENT_LENGTH = 255;
    private static final int MAX_RELEASE_LENGTH = 255;
    private static final int MAX_EXCEPTION_CLASS_LENGTH = 255;
    private static final int MAX_MESSAGE_LENGTH = 65_535;
    private static final int MAX_STACK_TRACE_LENGTH = 131_072;

    private final ErrorTrackerConfiguration config;
    private final HttpClient httpClient;

    ErrorTracker(ErrorTrackerConfiguration config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis((long) (config.getConnectTimeoutSeconds() * 1000)))
                .build();
    }

    public void report(ErrorEvent event) {
        try {
            Throwable throwable = event.getThrowable();
            if (throwable == null) {
                return;
            }

            for (Sanitizer sanitizer : config.getSanitizers()) {
                sanitizer.sanitize(event);
            }

            String exceptionClass = throwable.getClass().getName();
            String message = event.getMessage() != null ? event.getMessage() : "";
            String stackTrace = event.getStackTrace() != null ? event.getStackTrace() : "";

            send(exceptionClass, message, stackTrace);
        } catch (Exception e) {
            LOGGER.warning("ket4j: failed to report error: " + e.getMessage());
        }
    }

    private void send(String exceptionClass, String message, String stackTrace) {
        try {
            String url = config.getKendoUrl() + "/api/projects/" + config.getProjectId() + "/error-events";
            String json = buildJson(exceptionClass, message, stackTrace);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + config.getToken())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofMillis((long) (config.getTimeoutSeconds() * 1000)))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 202) {
                LOGGER.warning("ket4j: unexpected response status " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            LOGGER.warning("ket4j: send failed: " + e.getMessage());
        }
    }

    private String buildJson(String exceptionClass, String message, String stackTrace) {
        String environment = config.getEnvironment();
        String release = config.getRelease();

        StringBuilder builder = new StringBuilder("{");
        builder.append("\"environment\":").append(jsonString(truncate(environment, MAX_ENVIRONMENT_LENGTH)));
        if (release != null) {
            builder.append(",\"release\":").append(jsonString(truncate(release, MAX_RELEASE_LENGTH)));
        }
        builder.append(",\"exception_class\":").append(jsonString(truncate(exceptionClass, MAX_EXCEPTION_CLASS_LENGTH)));
        builder.append(",\"message\":").append(jsonString(truncate(message, MAX_MESSAGE_LENGTH)));
        builder.append(",\"stack_trace\":").append(jsonString(truncate(stackTrace, MAX_STACK_TRACE_LENGTH)));
        builder.append("}");
        return builder.toString();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String jsonString(String value) {
        return "\"" + escapeJson(value) + "\"";
    }

    private String escapeJson(String input) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char character = input.charAt(i);
            switch (character) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (character < 0x20) { // control characters below U+0020
                        builder.append(String.format("\\u%04x", (int) character));
                    } else {
                        builder.append(character);
                    }
                }
            }
        }
        return builder.toString();
    }
}
