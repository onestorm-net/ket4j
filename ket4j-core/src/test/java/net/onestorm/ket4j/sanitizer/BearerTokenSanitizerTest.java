package net.onestorm.ket4j.sanitizer;

import net.onestorm.ket4j.TestErrorEvent;
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

    private String sanitizeMessage(String message) {
        TestErrorEvent event = new TestErrorEvent(message);
        sanitizer.sanitize(event);
        return event.getMessage();
    }

    @Test
    void redactsBearerToken() {
        assertThat(sanitizeMessage("Authorization: Bearer abc123")).isEqualTo("Authorization: [REDACTED:bearer]");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer", "BEARER", "bearer"})
    void isCaseInsensitive(String keyword) {
        assertThat(sanitizeMessage("Authorization: " + keyword + " secrettoken")).isEqualTo("Authorization: [REDACTED:bearer]");
    }

    @Test
    void redactsBearerWithTabSeparator() {
        assertThat(sanitizeMessage("Authorization: Bearer\tsecrettoken")).isEqualTo("Authorization: [REDACTED:bearer]");
    }

    @Test
    void redactsBareReuseOfToken() {
        String input = "Authorization: Bearer mysecret, also logged: mysecret";
        assertThat(sanitizeMessage(input)).isEqualTo("Authorization: [REDACTED:bearer], also logged: [REDACTED:bearer]");
    }

    @Test
    void redactsMultipleBearerHeaders() {
        String input = "Bearer token1 Bearer token2";
        assertThat(sanitizeMessage(input)).isEqualTo("[REDACTED:bearer] [REDACTED:bearer]");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "no authorization here",
        "BearerWithoutSpace abc",
        ""
    })
    void doesNotSanitizeNonMatches(String input) {
        assertThat(sanitizeMessage(input)).isEqualTo(input);
    }

    @Test
    void doesNotReuseTokenAcrossFields() {
        TestErrorEvent event = new TestErrorEvent("Authorization: Bearer mysecret");
        event.setStackTrace("also logged: mysecret");

        sanitizer.sanitize(event);

        assertThat(event.getMessage()).isEqualTo("Authorization: [REDACTED:bearer]");
        assertThat(event.getStackTrace()).isEqualTo("also logged: mysecret");
    }
}
