package net.onestorm.ket4j.sanitizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class DsnPasswordSanitizerTest {

    private DsnPasswordSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new DsnPasswordSanitizer();
    }

    @Test
    void redactsPasswordInMysqlDsn() {
        assertThat(sanitizer.sanitize("mysql://user:secretpass@localhost/db"))
                .isEqualTo("mysql://user:[REDACTED:dsn-password]@localhost/db");
    }

    @Test
    void redactsPasswordInPostgresDsn() {
        assertThat(sanitizer.sanitize("postgresql://user:s3cr3t!pass@db.host:5432/mydb"))
                .isEqualTo("postgresql://user:[REDACTED:dsn-password]@db.host:5432/mydb");
    }

    @Test
    void redactsPasswordInHttpsUrl() {
        assertThat(sanitizer.sanitize("https://admin:hunter2@example.com/path"))
                .isEqualTo("https://admin:[REDACTED:dsn-password]@example.com/path");
    }

    @Test
    void redactsMultipleDsns() {
        String input = "primary: mysql://a:pass1@host1/db secondary: mysql://b:pass2@host2/db";
        assertThat(sanitizer.sanitize(input))
                .isEqualTo("primary: mysql://a:[REDACTED:dsn-password]@host1/db secondary: mysql://b:[REDACTED:dsn-password]@host2/db");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "no dsn here",
        "mysql://user@host/db",
        "https://example.com/path",
        ""
    })
    void doesNotSanitizeNonMatches(String input) {
        assertThat(sanitizer.sanitize(input)).isEqualTo(input);
    }
}
