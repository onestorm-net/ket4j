package net.onestorm.ket4j.log4j2;

import net.onestorm.ket4j.ErrorEvent;
import net.onestorm.ket4j.util.ExceptionUtil;
import org.apache.logging.log4j.core.LogEvent;

public class Log4j2ErrorEvent implements ErrorEvent {

    private String message;
    private final Throwable throwable;
    private String exceptionMessage;
    private String stackTrace;

    public Log4j2ErrorEvent(LogEvent event) {
        this.message = event.getMessage().getFormattedMessage();
        this.throwable = event.getThrown();
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
