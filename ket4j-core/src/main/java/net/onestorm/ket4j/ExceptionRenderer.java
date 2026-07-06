package net.onestorm.ket4j;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class ExceptionRenderer {

    private ExceptionRenderer() {
    }

    public static String exceptionMessageOf(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null) {
            return "";
        }
        return throwable.getMessage();
    }

    public static String stackTraceOf(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
