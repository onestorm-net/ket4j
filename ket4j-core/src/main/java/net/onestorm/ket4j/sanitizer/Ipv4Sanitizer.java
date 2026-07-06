package net.onestorm.ket4j.sanitizer;

import net.onestorm.ket4j.ErrorEvent;
import net.onestorm.ket4j.util.ErrorEventUtil;

import java.util.regex.Pattern;

public class Ipv4Sanitizer implements Sanitizer {

    private static final Pattern PATTERN = Pattern.compile(
            "\\b(?:(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\b"
    );

    @Override
    public void sanitize(ErrorEvent event) {
        ErrorEventUtil.applyToTextFields(event, this::redact);
    }

    private String redact(String input) {
        return PATTERN.matcher(input).replaceAll("[REDACTED:ip]");
    }
}
