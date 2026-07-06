package net.onestorm.ket4j.log4j2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Log4j2ErrorEventTest {

    @Test
    void extractsMessageAndThrowableFromLogEvent() {
        RuntimeException exception = new RuntimeException("boom");
        LogEvent logEvent = buildEvent("something broke", exception);

        Log4j2ErrorEvent event = new Log4j2ErrorEvent(logEvent);

        assertThat(event.getMessage()).isEqualTo("something broke");
        assertThat(event.getThrowable()).isSameAs(exception);
        assertThat(event.getExceptionMessage()).isEqualTo("boom");
        assertThat(event.getStackTrace()).contains("java.lang.RuntimeException: boom");
    }

    @Test
    void handlesNullThrowable() {
        LogEvent logEvent = buildEvent("no exception here", null);

        Log4j2ErrorEvent event = new Log4j2ErrorEvent(logEvent);

        assertThat(event.getThrowable()).isNull();
        assertThat(event.getExceptionMessage()).isEmpty();
        assertThat(event.getStackTrace()).isEmpty();
    }

    @Test
    void settersOverrideFields() {
        Log4j2ErrorEvent event = new Log4j2ErrorEvent(buildEvent("original", null));

        event.setMessage("changed message");
        event.setExceptionMessage("changed exception message");
        event.setStackTrace("changed stack trace");

        assertThat(event.getMessage()).isEqualTo("changed message");
        assertThat(event.getExceptionMessage()).isEqualTo("changed exception message");
        assertThat(event.getStackTrace()).isEqualTo("changed stack trace");
    }

    private LogEvent buildEvent(String message, Throwable throwable) {
        return Log4jLogEvent.newBuilder()
                .setLevel(Level.ERROR)
                .setMessage(new SimpleMessage(message))
                .setThrown(throwable)
                .build();
    }
}
