package net.onestorm.ket4j.sanitizer;

import java.util.regex.Pattern;

public class Ipv4Sanitizer implements Sanitizer {

    private static final Pattern PATTERN = Pattern.compile(
            "\\b(?:(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\b"
    );

    @Override
    public String sanitize(String input) {
        return PATTERN.matcher(input).replaceAll("[REDACTED:ip]");
    }
}
