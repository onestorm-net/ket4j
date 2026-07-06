package net.onestorm.ket4j;

public interface ErrorEvent {

    String getMessage();

    void setMessage(String message);

    Throwable getThrowable();

    String getExceptionMessage();

    void setExceptionMessage(String exceptionMessage);

    String getStackTrace();

    void setStackTrace(String stackTrace);
}
