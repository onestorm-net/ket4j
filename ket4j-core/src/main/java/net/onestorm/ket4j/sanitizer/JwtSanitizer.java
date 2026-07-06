package net.onestorm.ket4j.sanitizer;

import net.onestorm.ket4j.ErrorEvent;
import net.onestorm.ket4j.util.ErrorEventUtil;

import java.util.regex.Pattern;

public class JwtSanitizer implements Sanitizer {

    private static final Pattern PATTERN = Pattern.compile(
            "eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"
    );

    @Override
    public void sanitize(ErrorEvent event) {
        ErrorEventUtil.applyToTextFields(event, this::redact);
    }

    private String redact(String input) {
        return PATTERN.matcher(input).replaceAll("[REDACTED:jwt]");
    }
}
