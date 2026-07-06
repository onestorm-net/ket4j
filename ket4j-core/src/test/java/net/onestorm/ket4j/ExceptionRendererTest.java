package net.onestorm.ket4j;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionRendererTest {

    @Test
    void constructorIsPrivateAndUnused() throws ReflectiveOperationException {
        Constructor<ExceptionRenderer> constructor = ExceptionRenderer.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThat(constructor.newInstance()).isNotNull();
    }

    @Test
    void exceptionMessageOfReturnsThrowableMessage() {
        RuntimeException exception = new RuntimeException("boom");
        assertThat(ExceptionRenderer.exceptionMessageOf(exception)).isEqualTo("boom");
    }

    @Test
    void exceptionMessageOfReturnsEmptyStringWhenThrowableIsNull() {
        assertThat(ExceptionRenderer.exceptionMessageOf(null)).isEmpty();
    }

    @Test
    void exceptionMessageOfReturnsEmptyStringWhenThrowableMessageIsNull() {
        RuntimeException exception = new RuntimeException();
        assertThat(ExceptionRenderer.exceptionMessageOf(exception)).isEmpty();
    }

    @Test
    void stackTraceOfRendersFullThrowableTextIncludingCause() {
        RuntimeException cause = new RuntimeException("root cause");
        RuntimeException exception = new RuntimeException("boom", cause);

        String stackTrace = ExceptionRenderer.stackTraceOf(exception);

        assertThat(stackTrace)
                .contains("java.lang.RuntimeException: boom")
                .contains("Caused by: java.lang.RuntimeException: root cause");
    }

    @Test
    void stackTraceOfReturnsEmptyStringWhenThrowableIsNull() {
        assertThat(ExceptionRenderer.stackTraceOf(null)).isEmpty();
    }
}
