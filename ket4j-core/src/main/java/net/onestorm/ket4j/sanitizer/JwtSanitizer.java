package net.onestorm.ket4j.sanitizer;

import java.util.regex.Pattern;

public class JwtSanitizer implements Sanitizer {

    private static final Pattern PATTERN = Pattern.compile(
            "eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"
    );

    @Override
    public String sanitize(String input) {
        return PATTERN.matcher(input).replaceAll("[REDACTED:jwt]");
    }
}
