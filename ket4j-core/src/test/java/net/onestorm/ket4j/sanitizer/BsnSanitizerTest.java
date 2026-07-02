package net.onestorm.ket4j.sanitizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class BsnSanitizerTest {

    // Valid BSNs verified with elfproef (sum divisible by 11, sum != 0):
    // 123456782: sum=154, 111222333: sum=66, 999999990: sum=396

    private BsnSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new BsnSanitizer();
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456782", "111222333", "999999990"})
    void redactsValidBsnInDigitRun(String bsn) {
        assertThat(sanitizer.sanitize(bsn)).isEqualTo("[REDACTED:bsn]");
    }

    @Test
    void doesNotRedactInvalidBsnDigitRun() {
        assertThat(sanitizer.sanitize("123456789")).isEqualTo("123456789");
    }

    @Test
    void doesNotRedactAllZeros() {
        assertThat(sanitizer.sanitize("000000000")).isEqualTo("000000000");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123.456.782", "123 456 782", "123-456-782"})
    void redactsValidGroupedBsn(String grouped) {
        assertThat(sanitizer.sanitize(grouped)).isEqualTo("[REDACTED:bsn]");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123.456.789", "000.000.000"})
    void doesNotRedactInvalidGroupedBsn(String grouped) {
        assertThat(sanitizer.sanitize(grouped)).isEqualTo(grouped);
    }

    @Test
    void redactsValidBsnWindowWithinLongerDigitRun() {
        // 1234567821 — window at pos 0 (123456782) is valid, remainder is '1'
        assertThat(sanitizer.sanitize("1234567821")).isEqualTo("[REDACTED:bsn]1");
    }

    @Test
    void doesNotSanitizeShortDigitRun() {
        assertThat(sanitizer.sanitize("12345678")).isEqualTo("12345678");
    }

    @Test
    void doesNotSanitizeNonNumericInput() {
        assertThat(sanitizer.sanitize("no bsn here")).isEqualTo("no bsn here");
    }

}
