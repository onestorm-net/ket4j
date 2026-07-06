package net.onestorm.ket4j.sanitizer;

import net.onestorm.ket4j.TestErrorEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class EmailSanitizerTest {

    private EmailSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new EmailSanitizer();
    }

    private String sanitizeMessage(String message) {
        TestErrorEvent event = new TestErrorEvent(message);
        sanitizer.sanitize(event);
        return event.getMessage();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "user@example.com",
        "user.name+tag@sub.domain.org",
        "first.last@company.co.uk"
    })
    void redactsEmails(String email) {
        assertThat(sanitizeMessage(email)).isEqualTo("[REDACTED:email]");
    }

    @Test
    void redactsEmailEmbeddedInText() {
        assertThat(sanitizeMessage("contact admin@example.com for help"))
                .isEqualTo("contact [REDACTED:email] for help");
    }

    @Test
    void redactsMultipleEmails() {
        assertThat(sanitizeMessage("a@x.com and b@y.com"))
                .isEqualTo("[REDACTED:email] and [REDACTED:email]");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "not an email",
        "@missinglocal.com",
        "missingat.com",
        ""
    })
    void doesNotSanitizeNonMatches(String input) {
        assertThat(sanitizeMessage(input)).isEqualTo(input);
    }
}
