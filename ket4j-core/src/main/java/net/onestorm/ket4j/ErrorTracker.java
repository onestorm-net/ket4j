package net.onestorm.ket4j;

import net.onestorm.ket4j.sanitizer.Sanitizer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
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

    public void report(Throwable throwable, String message) {
        try {
            List<Sanitizer> sanitizers = config.getSanitizers();

            String sanitizedMessage = message != null ? message : "";
            for (Sanitizer sanitizer : sanitizers) {
                sanitizedMessage = sanitizer.sanitize(sanitizedMessage, throwable);
            }

            String exceptionClass = throwable != null ? throwable.getClass().getName() : null;

            String sanitizedStackTrace = null;
            if (throwable != null) {
                sanitizedStackTrace = stackTraceOf(throwable);
                for (Sanitizer sanitizer : sanitizers) {
                    sanitizedStackTrace = sanitizer.sanitize(sanitizedStackTrace);
                }
            }

            ErrorEventPayload event = new ErrorEventPayload(
                    config.getEnvironment(),
                    config.getRelease(),
                    exceptionClass,
                    sanitizedMessage,
                    sanitizedStackTrace
            );

            send(event);
        } catch (Exception e) {
            LOGGER.warning("ket4j: failed to report error: " + e.getMessage());
        }
    }

    private void send(ErrorEventPayload event) {
        try {
            String url = config.getKendoUrl() + "/api/projects/" + config.getProjectId() + "/error-events";
            String json = buildJson(event);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + config.getToken())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofMillis((long) (config.getTimeoutSeconds() * 1000)))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 202) {
                LOGGER.warning("ket4j: unexpected response status " + response.statusCode());
            }
        } catch (Exception e) {
            LOGGER.warning("ket4j: send failed: " + e.getMessage());
        }
    }

    private String buildJson(ErrorEventPayload event) {
        StringBuilder builder = new StringBuilder("{");
        builder.append("\"environment\":").append(jsonString(truncate(event.environment(), MAX_ENVIRONMENT_LENGTH)));
        if (event.release() != null) {
            builder.append(",\"release\":").append(jsonString(truncate(event.release(), MAX_RELEASE_LENGTH)));
        }
        String exceptionClass = event.exceptionClass() != null ? truncate(event.exceptionClass(), MAX_EXCEPTION_CLASS_LENGTH) : "none";
        builder.append(",\"exception_class\":").append(jsonString(exceptionClass));
        builder.append(",\"message\":").append(jsonString(truncate(event.message(), MAX_MESSAGE_LENGTH)));
        builder.append(",\"stack_trace\":").append(jsonString(truncate(event.stackTrace(), MAX_STACK_TRACE_LENGTH)));
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
        if (input == null) {
            return "";
        }
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

    private String stackTraceOf(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
