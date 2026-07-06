package net.onestorm.ket4j.sanitizer;

import net.onestorm.ket4j.ErrorEvent;
import net.onestorm.ket4j.util.ErrorEventUtil;

import java.util.regex.Pattern;

public class EmailSanitizer implements Sanitizer {

    private static final Pattern PATTERN = Pattern.compile(
            "[\\p{L}\\p{N}._%+\\-]+@[\\p{L}\\p{N}.\\-]+\\.[\\p{L}]{2,}"
    );

    @Override
    public void sanitize(ErrorEvent event) {
        ErrorEventUtil.applyToTextFields(event, this::redact);
    }

    private String redact(String input) {
        return PATTERN.matcher(input).replaceAll("[REDACTED:email]");
    }
}
