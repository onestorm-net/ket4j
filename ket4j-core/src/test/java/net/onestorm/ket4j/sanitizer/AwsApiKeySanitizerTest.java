package net.onestorm.ket4j.sanitizer;

import net.onestorm.ket4j.TestErrorEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class AwsApiKeySanitizerTest {

    private AwsApiKeySanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new AwsApiKeySanitizer();
    }

    private String sanitizeMessage(String message) {
        TestErrorEvent event = new TestErrorEvent(message);
        sanitizer.sanitize(event);
        return event.getMessage();
    }

    @Test
    void redactsAwsKey() {
        assertThat(sanitizeMessage("AWS key: AKIAIOSFODNN7EXAMPLE"))
                .isEqualTo("AWS key: [REDACTED:api-key]");
    }

    @Test
    void redactsMultipleAwsKeys() {
        String input = "AKIAIOSFODNN7EXAMPLE and AKIAI0SOFODNN7EXAMPL";
        assertThat(sanitizeMessage(input)).isEqualTo("[REDACTED:api-key] and [REDACTED:api-key]");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "AKIA123",
        "AKIAAAAAAAAAAAAAAA",
        "no key here",
        ""
    })
    void doesNotSanitizeNonMatches(String input) {
        assertThat(sanitizeMessage(input)).isEqualTo(input);
    }
}
