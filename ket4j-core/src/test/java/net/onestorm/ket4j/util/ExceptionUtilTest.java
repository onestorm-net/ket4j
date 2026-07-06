package net.onestorm.ket4j.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionUtilTest {

    @Test
    void constructorIsPrivateAndUnused() throws ReflectiveOperationException {
        Constructor<ExceptionUtil> constructor = ExceptionUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThat(constructor.newInstance()).isNotNull();
    }

    @Test
    void exceptionMessageOfReturnsThrowableMessage() {
        RuntimeException exception = new RuntimeException("boom");
        assertThat(ExceptionUtil.exceptionMessageOf(exception)).isEqualTo("boom");
    }

    @Test
    void exceptionMessageOfReturnsEmptyStringWhenThrowableIsNull() {
        assertThat(ExceptionUtil.exceptionMessageOf(null)).isEmpty();
    }

    @Test
    void exceptionMessageOfReturnsEmptyStringWhenThrowableMessageIsNull() {
        RuntimeException exception = new RuntimeException();
        assertThat(ExceptionUtil.exceptionMessageOf(exception)).isEmpty();
    }

    @Test
    void stackTraceOfRendersFullThrowableTextIncludingCause() {
        RuntimeException cause = new RuntimeException("root cause");
        RuntimeException exception = new RuntimeException("boom", cause);

        String stackTrace = ExceptionUtil.stackTraceOf(exception);

        assertThat(stackTrace)
                .contains("java.lang.RuntimeException: boom")
                .contains("Caused by: java.lang.RuntimeException: root cause");
    }

    @Test
    void stackTraceOfReturnsEmptyStringWhenThrowableIsNull() {
        assertThat(ExceptionUtil.stackTraceOf(null)).isEmpty();
    }
}
