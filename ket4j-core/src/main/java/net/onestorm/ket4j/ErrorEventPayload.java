package net.onestorm.ket4j;

public record ErrorEventPayload(
        String environment,
        String release,
        String exceptionClass,
        String message,
        String stackTrace
) {}
