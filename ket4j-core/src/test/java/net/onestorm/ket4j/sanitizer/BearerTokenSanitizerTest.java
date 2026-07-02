package net.onestorm.ket4j.sanitizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class BearerTokenSanitizerTest {

    private BearerTokenSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new BearerTokenSanitizer();
    }

    @Test
    void redactsBearerToken() {
        assertThat(sanitizer.sanitize("Authorization: Bearer abc123")).isEqualTo("Authorization: [REDACTED:bearer]");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer", "BEARER", "bearer"})
    void isCaseInsensitive(String keyword) {
        assertThat(sanitizer.sanitize("Authorization: " + keyword + " secrettoken")).isEqualTo("Authorization: [REDACTED:bearer]");
    }

    @Test
    void redactsBearerWithTabSeparator() {
        assertThat(sanitizer.sanitize("Authorization: Bearer\tsecrettoken")).isEqualTo("Authorization: [REDACTED:bearer]");
    }

    @Test
    void redactsBareReuseOfToken() {
        String input = "Authorization: Bearer mysecret, also logged: mysecret";
        assertThat(sanitizer.sanitize(input)).isEqualTo("Authorization: [REDACTED:bearer], also logged: [REDACTED:bearer]");
    }

    @Test
    void redactsMultipleBearerHeaders() {
        String input = "Bearer token1 Bearer token2";
        assertThat(sanitizer.sanitize(input)).isEqualTo("[REDACTED:bearer] [REDACTED:bearer]");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "no authorization here",
        "BearerWithoutSpace abc",
        ""
    })
    void doesNotSanitizeNonMatches(String input) {
        assertThat(sanitizer.sanitize(input)).isEqualTo(input);
    }
}
