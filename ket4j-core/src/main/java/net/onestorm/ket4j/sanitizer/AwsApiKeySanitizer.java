package net.onestorm.ket4j.sanitizer;

import net.onestorm.ket4j.ErrorEvent;
import net.onestorm.ket4j.util.ErrorEventUtil;

import java.util.regex.Pattern;

public class AwsApiKeySanitizer implements Sanitizer {

    private static final Pattern PATTERN = Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b");

    @Override
    public void sanitize(ErrorEvent event) {
        ErrorEventUtil.applyToTextFields(event, AwsApiKeySanitizer::redact);
    }

    private static String redact(String input) {
        return PATTERN.matcher(input).replaceAll("[REDACTED:api-key]");
    }
}
