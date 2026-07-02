package net.onestorm.ket4j.sanitizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DsnPasswordSanitizer implements Sanitizer {

    // Matches scheme://user:pass@host — only the password (group 2) is redacted.
    private static final Pattern PATTERN = Pattern.compile(
            "([a-zA-Z][a-zA-Z0-9+.\\-]*://[^:/?#\\s@]+:)([^@/?#\\s]+)(@)"
    );

    @Override
    public String sanitize(String input) {
        return PATTERN.matcher(input).replaceAll(match ->
                Matcher.quoteReplacement(match.group(1)) + "[REDACTED:dsn-password]" + match.group(3)
        );
    }
}
