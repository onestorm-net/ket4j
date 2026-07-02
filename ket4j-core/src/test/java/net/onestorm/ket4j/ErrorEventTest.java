package net.onestorm.ket4j;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorEventTest {

    @Test
    void accessorsReturnConstructorValues() {
        ErrorEvent event = new ErrorEvent("production", "1.0.0", "java.lang.RuntimeException", "oops", "at Foo.java:1");
        assertThat(event.environment()).isEqualTo("production");
        assertThat(event.release()).isEqualTo("1.0.0");
        assertThat(event.exceptionClass()).isEqualTo("java.lang.RuntimeException");
        assertThat(event.message()).isEqualTo("oops");
        assertThat(event.stackTrace()).isEqualTo("at Foo.java:1");
    }

    @Test
    void equalEventsAreEqual() {
        ErrorEvent a = new ErrorEvent("prod", null, "Foo", "msg", "trace");
        ErrorEvent b = new ErrorEvent("prod", null, "Foo", "msg", "trace");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void toStringContainsFieldValues() {
        ErrorEvent event = new ErrorEvent("prod", "1.0", "Foo", "msg", "trace");
        assertThat(event.toString()).contains("prod").contains("1.0").contains("Foo");
    }
}
