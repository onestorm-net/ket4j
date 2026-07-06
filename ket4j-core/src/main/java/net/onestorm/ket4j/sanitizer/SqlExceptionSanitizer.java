package net.onestorm.ket4j.sanitizer;

import net.onestorm.ket4j.ErrorEvent;

import java.sql.SQLException;

public class SqlExceptionSanitizer implements Sanitizer {

    private static final int MAX_CAUSE_DEPTH = 10;

    @Override
    public void sanitize(ErrorEvent event) {
        SQLException exception = findSqlException(event.getThrowable());
        if (exception == null) {
            return;
        }

        String className = exception.getClass().getName();
        String sqlState = exception.getSQLState() != null ? exception.getSQLState() : "unknown";
        int errorCode = exception.getErrorCode();
        String replacement = className + " [SQLSTATE " + sqlState + "] [error code " + errorCode + "]";

        event.setExceptionMessage(replacement);

        String originalMessage = exception.getMessage();
        String stackTrace = event.getStackTrace();
        if (originalMessage != null && !originalMessage.isEmpty() && stackTrace != null) {
            event.setStackTrace(stackTrace.replace(originalMessage, replacement));
        }
    }

    private SQLException findSqlException(Throwable throwable) {
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth < MAX_CAUSE_DEPTH) {
            if (current instanceof SQLException exception) {
                return exception;
            }
            current = current.getCause();
            depth++;
        }
        return null;
    }
}
