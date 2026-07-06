package net.onestorm.ket4j.sanitizer;

import net.onestorm.ket4j.TestErrorEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class Ipv4SanitizerTest {

    private Ipv4Sanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new Ipv4Sanitizer();
    }

    private String sanitizeMessage(String message) {
        TestErrorEvent event = new TestErrorEvent(message);
        sanitizer.sanitize(event);
        return event.getMessage();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "192.168.1.1",
        "0.0.0.0",
        "255.255.255.255",
        "10.0.0.1"
    })
    void redactsValidIpAddresses(String ip) {
        assertThat(sanitizeMessage(ip)).isEqualTo("[REDACTED:ip]");
    }

    @Test
    void redactsIpEmbeddedInText() {
        assertThat(sanitizeMessage("connecting from 192.168.0.1 to server"))
                .isEqualTo("connecting from [REDACTED:ip] to server");
    }

    @Test
    void redactsMultipleIps() {
        assertThat(sanitizeMessage("192.168.0.1 and 10.0.0.2"))
                .isEqualTo("[REDACTED:ip] and [REDACTED:ip]");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "256.0.0.1",
        "192.168.1.999",
        "192.168.1",
        "not an ip",
        ""
    })
    void doesNotSanitizeNonMatches(String input) {
        assertThat(sanitizeMessage(input)).isEqualTo(input);
    }
}
