package net.onestorm.ket4j.sanitizer;

import net.onestorm.ket4j.ErrorEvent;
import net.onestorm.ket4j.util.ErrorEventUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BsnSanitizer implements Sanitizer {

    private static final Pattern GROUPED = Pattern.compile(
            "\\d{3}[.\\s\\-]\\d{3}[.\\s\\-]\\d{3}"
    );
    private static final Pattern DIGIT_RUN = Pattern.compile("\\d{9,}");

    @Override
    public void sanitize(ErrorEvent event) {
        ErrorEventUtil.applyToTextFields(event, BsnSanitizer::redact);
    }

    private static String redact(String input) {
        return scrubDigitRuns(scrubGrouped(input));
    }

    private static String scrubGrouped(String input) {
        Matcher matcher = GROUPED.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String digits = matcher.group().replaceAll("\\D", "");
            String replacement = isValidBsn(digits) ? "[REDACTED:bsn]" : Matcher.quoteReplacement(matcher.group());
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String scrubDigitRuns(String input) {
        Matcher matcher = DIGIT_RUN.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(redactWindows(matcher.group())));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    // Scans left-to-right for non-overlapping 9-digit windows that pass elfproef.
    private static String redactWindows(String digits) {
        StringBuilder result = new StringBuilder();
        int pos = 0;
        while (pos <= digits.length() - 9) {
            String window = digits.substring(pos, pos + 9);
            if (isValidBsn(window)) {
                result.append("[REDACTED:bsn]");
                pos += 9;
            } else {
                result.append(digits.charAt(pos));
                pos++;
            }
        }
        result.append(digits.substring(pos));
        return result.toString();
    }

    // Dutch eleven-test (elfproef): weights 9..2 for positions 0-7, weight -1 for position 8.
    // Sum must be a non-zero multiple of 11.
    private static boolean isValidBsn(String digits) {
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            sum += (9 - i) * (digits.charAt(i) - '0');
        }
        sum -= (digits.charAt(8) - '0');
        return sum != 0 && sum % 11 == 0;
    }
}
