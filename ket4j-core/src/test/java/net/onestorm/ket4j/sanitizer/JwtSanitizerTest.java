package net.onestorm.ket4j.sanitizer;

import net.onestorm.ket4j.TestErrorEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class JwtSanitizerTest {

    private JwtSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new JwtSanitizer();
    }

    private String sanitizeMessage(String message) {
        TestErrorEvent event = new TestErrorEvent(message);
        sanitizer.sanitize(event);
        return event.getMessage();
    }

    @Test
    void redactsJwt() {
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        assertThat(sanitizeMessage(jwt)).isEqualTo("[REDACTED:jwt]");
    }

    @Test
    void redactsJwtEmbeddedInLargerString() {
        String input = "token=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0.abc123 and more text";
        assertThat(sanitizeMessage(input)).isEqualTo("token=[REDACTED:jwt] and more text");
    }

    @Test
    void redactsMultipleJwts() {
        String input = "eyJhbGciOiJIUzI1NiJ9.eyJhIn0.aaa eyJhbGciOiJIUzI1NiJ9.eyJiIn0.bbb";
        assertThat(sanitizeMessage(input)).isEqualTo("[REDACTED:jwt] [REDACTED:jwt]");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "no jwt here",
        "eyJhbGci.onlytwoparts",
        "Bearer token",
        ""
    })
    void doesNotSanitizeNonMatches(String input) {
        assertThat(sanitizeMessage(input)).isEqualTo(input);
    }

    @Test
    void redactsJwtInExceptionMessageAndStackTrace() {
        TestErrorEvent event = new TestErrorEvent("plain message");
        event.setExceptionMessage("token eyJhbGciOiJIUzI1NiJ9.eyJhIn0.aaa");
        event.setStackTrace("trace eyJhbGciOiJIUzI1NiJ9.eyJiIn0.bbb");

        sanitizer.sanitize(event);

        assertThat(event.getExceptionMessage()).isEqualTo("token [REDACTED:jwt]");
        assertThat(event.getStackTrace()).isEqualTo("trace [REDACTED:jwt]");
    }
}
