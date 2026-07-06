package net.onestorm.ket4j.util;

import net.onestorm.ket4j.ErrorEvent;

import java.util.function.UnaryOperator;

public final class ErrorEventUtil {

    private ErrorEventUtil() {
    }

    public static void applyToTextFields(ErrorEvent event, UnaryOperator<String> transform) {
        event.setMessage(transform.apply(nullToEmpty(event.getMessage())));
        event.setExceptionMessage(transform.apply(nullToEmpty(event.getExceptionMessage())));
        event.setStackTrace(transform.apply(nullToEmpty(event.getStackTrace())));
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
