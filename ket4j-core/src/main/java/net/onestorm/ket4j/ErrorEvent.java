package net.onestorm.ket4j;

public record ErrorEvent(
        String environment,
        String release,
        String exceptionClass,
        String message,
        String stackTrace
) {}
