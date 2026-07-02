package net.onestorm.ket4j.sanitizer;

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

    @Test
    void redactsAwsKey() {
        assertThat(sanitizer.sanitize("AWS key: AKIAIOSFODNN7EXAMPLE"))
                .isEqualTo("AWS key: [REDACTED:api-key]");
    }

    @Test
    void redactsMultipleAwsKeys() {
        String input = "AKIAIOSFODNN7EXAMPLE and AKIAI0SOFODNN7EXAMPL";
        assertThat(sanitizer.sanitize(input)).isEqualTo("[REDACTED:api-key] and [REDACTED:api-key]");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "AKIA123",
        "AKIAAAAAAAAAAAAAAA",
        "no key here",
        ""
    })
    void doesNotSanitizeNonMatches(String input) {
        assertThat(sanitizer.sanitize(input)).isEqualTo(input);
    }
}
