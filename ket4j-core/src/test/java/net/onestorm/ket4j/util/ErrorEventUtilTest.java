package net.onestorm.ket4j.util;

import net.onestorm.ket4j.TestErrorEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorEventUtilTest {

    @Test
    void constructorIsPrivateAndUnused() throws ReflectiveOperationException {
        Constructor<ErrorEventUtil> constructor = ErrorEventUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThat(constructor.newInstance()).isNotNull();
    }

    @Test
    void appliesTransformToAllThreeTextFields() {
        TestErrorEvent event = new TestErrorEvent("message");
        event.setExceptionMessage("exceptionMessage");
        event.setStackTrace("stackTrace");

        ErrorEventUtil.applyToTextFields(event, String::toUpperCase);

        assertThat(event.getMessage()).isEqualTo("MESSAGE");
        assertThat(event.getExceptionMessage()).isEqualTo("EXCEPTIONMESSAGE");
        assertThat(event.getStackTrace()).isEqualTo("STACKTRACE");
    }

    @Test
    void treatsNullFieldsAsEmptyString() {
        TestErrorEvent event = new TestErrorEvent(null);
        event.setExceptionMessage(null);
        event.setStackTrace(null);

        ErrorEventUtil.applyToTextFields(event, input -> input + "!");

        assertThat(event.getMessage()).isEqualTo("!");
        assertThat(event.getExceptionMessage()).isEqualTo("!");
        assertThat(event.getStackTrace()).isEqualTo("!");
    }
}
