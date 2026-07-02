package net.onestorm.ket4j;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ErrorTrackerProviderTest {

    @BeforeEach
    @AfterEach
    void resetInstance() throws Exception {
        Field field = ErrorTrackerProvider.class.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        AtomicReference<?> instance = (AtomicReference<?>) field.get(null);
        instance.set(null);
    }

    @Test
    void getInstanceBeforeInitializeThrows() {
        assertThatThrownBy(ErrorTrackerProvider::getInstance)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getInstanceAfterInitializeReturnsTracker() {
        ErrorTrackerProvider.initialize(validConfig());
        assertThat(ErrorTrackerProvider.getInstance()).isNotNull();
    }

    @Test
    void doubleInitializeThrows() {
        ErrorTrackerProvider.initialize(validConfig());
        assertThatThrownBy(() -> ErrorTrackerProvider.initialize(validConfig()))
                .isInstanceOf(IllegalStateException.class);
    }

    private ErrorTrackerConfiguration validConfig() {
        return ErrorTrackerConfiguration.builder()
                .kendoUrl("http://kendo.example.com")
                .projectId("proj")
                .token("tok")
                .build();
    }
}
