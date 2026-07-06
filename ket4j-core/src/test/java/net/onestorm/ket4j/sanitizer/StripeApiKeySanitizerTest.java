package net.onestorm.ket4j.sanitizer;

import net.onestorm.ket4j.TestErrorEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class StripeApiKeySanitizerTest {

    private StripeApiKeySanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new StripeApiKeySanitizer();
    }

    private String sanitizeMessage(String message) {
        TestErrorEvent event = new TestErrorEvent(message);
        sanitizer.sanitize(event);
        return event.getMessage();
    }

    @Test
    void redactsStripeKey() {
        assertThat(sanitizeMessage("key=sk_live_abcdefghij1234567890"))
                .isEqualTo("key=[REDACTED:api-key]");
    }

    @Test
    void redactsMultipleStripeKeys() {
        String input = "sk_live_aaaaaaaaaa sk_live_bbbbbbbbbb";
        assertThat(sanitizeMessage(input)).isEqualTo("[REDACTED:api-key] [REDACTED:api-key]");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "sk_test_abcdefghij",
        "sk_live_short",
        "no key here",
        ""
    })
    void doesNotSanitizeNonMatches(String input) {
        assertThat(sanitizeMessage(input)).isEqualTo(input);
    }
}
