package net.onestorm.ket4j.sanitizer;

import java.util.regex.Pattern;

public class EmailSanitizer implements Sanitizer {

    private static final Pattern PATTERN = Pattern.compile(
            "[\\p{L}\\p{N}._%+\\-]+@[\\p{L}\\p{N}.\\-]+\\.[\\p{L}]{2,}"
    );

    @Override
    public String sanitize(String input) {
        return PATTERN.matcher(input).replaceAll("[REDACTED:email]");
    }
}
