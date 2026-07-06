package net.onestorm.ket4j.sanitizer;

import net.onestorm.ket4j.ErrorEvent;
import net.onestorm.ket4j.util.ErrorEventUtil;

import java.util.regex.Pattern;

public class StripeApiKeySanitizer implements Sanitizer {

    private static final Pattern PATTERN = Pattern.compile("\\bsk_live_[A-Za-z0-9]{10,}\\b");

    @Override
    public void sanitize(ErrorEvent event) {
        ErrorEventUtil.applyToTextFields(event, StripeApiKeySanitizer::redact);
    }

    private static String redact(String input) {
        return PATTERN.matcher(input).replaceAll("[REDACTED:api-key]");
    }
}
