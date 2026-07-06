package net.onestorm.ket4j.sanitizer;

import net.onestorm.ket4j.ErrorEvent;
import net.onestorm.ket4j.util.ErrorEventUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class BearerTokenSanitizer implements Sanitizer {

    private static final Pattern PATTERN = Pattern.compile(
            "Bearer[ \\t]+([A-Za-z0-9._~+\\-]+=*)",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public void sanitize(ErrorEvent event) {
        ErrorEventUtil.applyToTextFields(event, this::redact);
    }

    private String redact(String input) {
        List<String> tokens = new ArrayList<>();

        String result = PATTERN.matcher(input).replaceAll(match -> {
            tokens.add(match.group(1));
            return "[REDACTED:bearer]";
        });

        for (String token : tokens) {
            result = result.replace(token, "[REDACTED:bearer]");
        }

        return result;
    }
}
