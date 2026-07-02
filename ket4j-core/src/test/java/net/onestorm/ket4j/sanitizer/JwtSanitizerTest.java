package net.onestorm.ket4j.sanitizer;

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

    @Test
    void redactsJwt() {
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        assertThat(sanitizer.sanitize(jwt)).isEqualTo("[REDACTED:jwt]");
    }

    @Test
    void redactsJwtEmbeddedInLargerString() {
        String input = "token=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0.abc123 and more text";
        assertThat(sanitizer.sanitize(input)).isEqualTo("token=[REDACTED:jwt] and more text");
    }

    @Test
    void redactsMultipleJwts() {
        String input = "eyJhbGciOiJIUzI1NiJ9.eyJhIn0.aaa eyJhbGciOiJIUzI1NiJ9.eyJiIn0.bbb";
        assertThat(sanitizer.sanitize(input)).isEqualTo("[REDACTED:jwt] [REDACTED:jwt]");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "no jwt here",
        "eyJhbGci.onlytwoparts",
        "Bearer token",
        ""
    })
    void doesNotSanitizeNonMatches(String input) {
        assertThat(sanitizer.sanitize(input)).isEqualTo(input);
    }

    @Test
    void throwableOverloadDelegatesToStringSanitize() {
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0.abc123";
        assertThat(sanitizer.sanitize(jwt, new RuntimeException("irrelevant"))).isEqualTo("[REDACTED:jwt]");
    }
}
