package net.onestorm.ket4j.sanitizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class SqlExceptionSanitizerTest {

    private SqlExceptionSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new SqlExceptionSanitizer();
    }

    @Test
    void stringSanitizeReturnsInputUnchanged() {
        assertThat(sanitizer.sanitize("SELECT * FROM users WHERE password='secret'"))
                .isEqualTo("SELECT * FROM users WHERE password='secret'");
    }

    @Test
    void nullThrowableReturnsInputUnchanged() {
        assertThat(sanitizer.sanitize("original message", null)).isEqualTo("original message");
    }

    @Test
    void nonSqlThrowableReturnsInputUnchanged() {
        assertThat(sanitizer.sanitize("original message", new IOException("connection reset")))
                .isEqualTo("original message");
    }

    @Test
    void sqlExceptionReplacesMessageWithFingerprint() {
        SQLException sqle = new SQLException("SELECT * FROM users -- injected sql", "23505", 1062);
        String result = sanitizer.sanitize("original message", sqle);
        assertThat(result).isEqualTo("java.sql.SQLException [SQLSTATE 23505] [error code 1062]");
    }

    @Test
    void sqlExceptionWrappedInRuntimeExceptionIsFound() {
        SQLException sqle = new SQLException("SELECT password FROM users", "42000", 1064);
        RuntimeException wrapper = new RuntimeException("database error", sqle);
        String result = sanitizer.sanitize("original", wrapper);
        assertThat(result).isEqualTo("java.sql.SQLException [SQLSTATE 42000] [error code 1064]");
    }

    @Test
    void nullSqlStateUsesUnknownFallback() {
        SQLException sqle = new SQLException("some sql error", null, 0);
        String result = sanitizer.sanitize("original", sqle);
        assertThat(result).isEqualTo("java.sql.SQLException [SQLSTATE unknown] [error code 0]");
    }
}
