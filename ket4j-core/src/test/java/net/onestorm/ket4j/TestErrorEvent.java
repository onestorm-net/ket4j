package net.onestorm.ket4j;

import net.onestorm.ket4j.util.ExceptionUtil;

public final class TestErrorEvent implements ErrorEvent {

    private String message;
    private final Throwable throwable;
    private String exceptionMessage;
    private String stackTrace;

    public TestErrorEvent(String message) {
        this(message, null);
    }

    public TestErrorEvent(String message, Throwable throwable) {
        this.message = message;
        this.throwable = throwable;
        this.exceptionMessage = ExceptionUtil.exceptionMessageOf(throwable);
        this.stackTrace = ExceptionUtil.stackTraceOf(throwable);
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public Throwable getThrowable() {
        return throwable;
    }

    @Override
    public String getExceptionMessage() {
        return exceptionMessage;
    }

    @Override
    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    @Override
    public String getStackTrace() {
        return stackTrace;
    }

    @Override
    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }
}
