package net.onestorm.ket4j.sanitizer;

import java.sql.SQLException;

public class SqlExceptionSanitizer implements Sanitizer {

    private static final int MAX_CAUSE_DEPTH = 10;

    @Override
    public String sanitize(String input) {
        return input;
    }

    @Override
    public String sanitize(String input, Throwable throwable) {
        SQLException exception = findSqlException(throwable);
        if (exception == null) {
            return input;
        }
        String className = exception.getClass().getName();
        String sqlState = exception.getSQLState() != null ? exception.getSQLState() : "unknown";
        int errorCode = exception.getErrorCode();
        return className + " [SQLSTATE " + sqlState + "] [error code " + errorCode + "]";
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
