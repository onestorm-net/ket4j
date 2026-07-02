package net.onestorm.ket4j.sanitizer;

import java.util.regex.Pattern;

public class AwsApiKeySanitizer implements Sanitizer {

    private static final Pattern PATTERN = Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b");

    @Override
    public String sanitize(String input) {
        return PATTERN.matcher(input).replaceAll("[REDACTED:api-key]");
    }
}
