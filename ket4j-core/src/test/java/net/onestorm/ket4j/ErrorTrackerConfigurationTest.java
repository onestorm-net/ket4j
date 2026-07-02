package net.onestorm.ket4j;

import net.onestorm.ket4j.sanitizer.PathSanitizer;
import net.onestorm.ket4j.sanitizer.Sanitizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ErrorTrackerConfigurationTest {

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankKendoUrlIsRejected(String value) {
        assertThatThrownBy(() -> ErrorTrackerConfiguration.builder()
                .kendoUrl(value)
                .projectId("proj")
                .token("tok")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kendoUrl");
    }

    @Test
    void nullKendoUrlIsRejected() {
        assertThatThrownBy(() -> ErrorTrackerConfiguration.builder()
                .projectId("proj")
                .token("tok")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kendoUrl");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankProjectIdIsRejected(String value) {
        assertThatThrownBy(() -> ErrorTrackerConfiguration.builder()
                .kendoUrl("http://kendo.example.com")
                .projectId(value)
                .token("tok")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectId");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankTokenIsRejected(String value) {
        assertThatThrownBy(() -> ErrorTrackerConfiguration.builder()
                .kendoUrl("http://kendo.example.com")
                .projectId("proj")
                .token(value)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("token");
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -1.0})
    void nonPositiveConnectTimeoutIsRejected(double value) {
        assertThatThrownBy(() -> ErrorTrackerConfiguration.builder()
                .kendoUrl("http://kendo.example.com")
                .projectId("proj")
                .token("tok")
                .connectTimeoutSeconds(value)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connectTimeoutSeconds");
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -1.0})
    void nonPositiveTimeoutIsRejected(double value) {
        assertThatThrownBy(() -> ErrorTrackerConfiguration.builder()
                .kendoUrl("http://kendo.example.com")
                .projectId("proj")
                .token("tok")
                .timeoutSeconds(value)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeoutSeconds");
    }

    @Test
    void defaultsAreApplied() {
        ErrorTrackerConfiguration config = ErrorTrackerConfiguration.builder()
                .kendoUrl("http://kendo.example.com")
                .projectId("proj")
                .token("tok")
                .build();

        assertThat(config.getEnvironment()).isEqualTo("production");
        assertThat(config.getRelease()).isNull();
        assertThat(config.getConnectTimeoutSeconds()).isEqualTo(2.0);
        assertThat(config.getTimeoutSeconds()).isEqualTo(5.0);
    }

    @Test
    void customEnvironmentIsUsed() {
        ErrorTrackerConfiguration config = ErrorTrackerConfiguration.builder()
                .kendoUrl("http://kendo.example.com")
                .projectId("proj")
                .token("tok")
                .environment("staging")
                .build();
        assertThat(config.getEnvironment()).isEqualTo("staging");
    }

    @Test
    void defaultSanitizerListHasNineEntries() {
        ErrorTrackerConfiguration config = ErrorTrackerConfiguration.builder()
                .kendoUrl("http://kendo.example.com")
                .projectId("proj")
                .token("tok")
                .build();
        assertThat(config.getSanitizers()).hasSize(9);
    }

    @Test
    void defaultSanitizerListEndsWithPathSanitizer() {
        ErrorTrackerConfiguration config = ErrorTrackerConfiguration.builder()
                .kendoUrl("http://kendo.example.com")
                .projectId("proj")
                .token("tok")
                .build();
        List<Sanitizer> sanitizers = config.getSanitizers();
        assertThat(sanitizers.get(sanitizers.size() - 1)).isInstanceOf(PathSanitizer.class);
    }

    @Test
    void customSanitizersListIsUsed() {
        List<Sanitizer> custom = List.of(input -> input.replace("secret", "[REDACTED]"));
        ErrorTrackerConfiguration config = ErrorTrackerConfiguration.builder()
                .kendoUrl("http://kendo.example.com")
                .projectId("proj")
                .token("tok")
                .sanitizers(custom)
                .build();
        assertThat(config.getSanitizers()).hasSize(1);
    }

    @Test
    void sanitizerListIsUnmodifiable() {
        ErrorTrackerConfiguration config = ErrorTrackerConfiguration.builder()
                .kendoUrl("http://kendo.example.com")
                .projectId("proj")
                .token("tok")
                .build();
        assertThatThrownBy(() -> config.getSanitizers().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
