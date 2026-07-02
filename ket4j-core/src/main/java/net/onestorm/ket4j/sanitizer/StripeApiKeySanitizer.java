package net.onestorm.ket4j.sanitizer;

import java.util.regex.Pattern;

public class StripeApiKeySanitizer implements Sanitizer {

    private static final Pattern PATTERN = Pattern.compile("\\bsk_live_[A-Za-z0-9]{10,}\\b");

    @Override
    public String sanitize(String input) {
        return PATTERN.matcher(input).replaceAll("[REDACTED:api-key]");
    }
}
