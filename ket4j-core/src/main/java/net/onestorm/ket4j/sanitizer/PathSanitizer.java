package net.onestorm.ket4j.sanitizer;

import net.onestorm.ket4j.ErrorEvent;
import net.onestorm.ket4j.util.ErrorEventUtil;

import java.util.regex.Pattern;

public class PathSanitizer implements Sanitizer {

    private static final Pattern HOME_USERNAME = Pattern.compile("/home/[^/\\s]+/");
    private static final Pattern MAC_USERNAME = Pattern.compile("/Users/[^/\\s]+/");

    private final String basePath;

    public PathSanitizer(String basePath) {
        this.basePath = (basePath == null || basePath.isBlank()) ? null : basePath;
    }

    @Override
    public void sanitize(ErrorEvent event) {
        ErrorEventUtil.applyToTextFields(event, this::redact);
    }

    private String redact(String input) {
        String result = basePath != null ? input.replace(basePath, "") : input;
        result = HOME_USERNAME.matcher(result).replaceAll("/home/[REDACTED:user]/");
        return MAC_USERNAME.matcher(result).replaceAll("/Users/[REDACTED:user]/");
    }
}
