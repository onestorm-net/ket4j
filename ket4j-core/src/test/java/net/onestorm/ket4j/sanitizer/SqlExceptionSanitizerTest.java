package net.onestorm.ket4j.sanitizer;

import net.onestorm.ket4j.TestErrorEvent;
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
    void nullThrowableDoesNothing() {
        TestErrorEvent event = new TestErrorEvent("original message", null);
        String originalExceptionMessage = event.getExceptionMessage();
        String originalStackTrace = event.getStackTrace();

        sanitizer.sanitize(event);

        assertThat(event.getMessage()).isEqualTo("original message");
        assertThat(event.getExceptionMessage()).isEqualTo(originalExceptionMessage);
        assertThat(event.getStackTrace()).isEqualTo(originalStackTrace);
    }

    @Test
    void nonSqlThrowableDoesNothing() {
        TestErrorEvent event = new TestErrorEvent("original message", new IOException("connection reset"));
        String originalExceptionMessage = event.getExceptionMessage();
        String originalStackTrace = event.getStackTrace();

        sanitizer.sanitize(event);

        assertThat(event.getExceptionMessage()).isEqualTo(originalExceptionMessage);
        assertThat(event.getStackTrace()).isEqualTo(originalStackTrace);
    }

    @Test
    void sqlExceptionSetsExceptionMessageToFingerprintAndLeavesLogMessageAlone() {
        SQLException sqle = new SQLException("SELECT * FROM users -- injected sql", "23505", 1062);
        TestErrorEvent event = new TestErrorEvent("original message", sqle);

        sanitizer.sanitize(event);

        assertThat(event.getMessage()).isEqualTo("original message");
        assertThat(event.getExceptionMessage()).isEqualTo("java.sql.SQLException [SQLSTATE 23505] [error code 1062]");
    }

    @Test
    void sqlExceptionScrubsOriginalMessageFromStackTrace() {
        SQLException sqle = new SQLException("SELECT * FROM users -- injected sql", "23505", 1062);
        TestErrorEvent event = new TestErrorEvent("original message", sqle);

        sanitizer.sanitize(event);

        assertThat(event.getStackTrace())
                .doesNotContain("SELECT * FROM users -- injected sql")
                .contains("java.sql.SQLException [SQLSTATE 23505] [error code 1062]");
    }

    @Test
    void sqlExceptionWrappedInRuntimeExceptionIsFoundAndScrubbed() {
        SQLException sqle = new SQLException("SELECT password FROM users", "42000", 1064);
        RuntimeException wrapper = new RuntimeException("database error", sqle);
        TestErrorEvent event = new TestErrorEvent("original", wrapper);

        sanitizer.sanitize(event);

        String fingerprint = "java.sql.SQLException [SQLSTATE 42000] [error code 1064]";
        assertThat(event.getExceptionMessage()).isEqualTo(fingerprint);
        assertThat(event.getStackTrace())
                .doesNotContain("SELECT password FROM users")
                .contains(fingerprint);
    }

    @Test
    void nullSqlStateUsesUnknownFallback() {
        SQLException sqle = new SQLException("some sql error", null, 0);
        TestErrorEvent event = new TestErrorEvent("original", sqle);

        sanitizer.sanitize(event);

        assertThat(event.getExceptionMessage()).isEqualTo("java.sql.SQLException [SQLSTATE unknown] [error code 0]");
    }

    @Test
    void sqlExceptionWithNullMessageLeavesStackTraceUnchangedButSetsExceptionMessage() {
        SQLException sqle = new SQLException((String) null, "08001", 500);
        TestErrorEvent event = new TestErrorEvent("original", sqle);
        String originalStackTrace = event.getStackTrace();

        sanitizer.sanitize(event);

        assertThat(event.getExceptionMessage()).isEqualTo("java.sql.SQLException [SQLSTATE 08001] [error code 500]");
        assertThat(event.getStackTrace()).isEqualTo(originalStackTrace);
    }
}
