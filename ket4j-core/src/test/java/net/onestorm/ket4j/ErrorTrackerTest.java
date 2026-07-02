package net.onestorm.ket4j;

import com.sun.net.httpserver.HttpServer;
import net.onestorm.ket4j.sanitizer.Sanitizer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ErrorTrackerTest {

    private HttpServer mockServer;
    private int mockPort;
    private final List<String> receivedBodies = new ArrayList<>();
    private volatile int responseCode = 202;

    @BeforeEach
    void startMockServer() throws IOException {
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        mockPort = mockServer.getAddress().getPort();
        mockServer.createContext("/", exchange -> {
            byte[] bytes = exchange.getRequestBody().readAllBytes();
            receivedBodies.add(new String(bytes, StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(responseCode, -1);
            exchange.close();
        });
        mockServer.start();
    }

    @AfterEach
    void stopMockServer() {
        mockServer.stop(0);
        receivedBodies.clear();
    }

    @Test
    void reportSendsCorrectPayload() {
        ErrorTracker tracker = new ErrorTracker(configForMock());
        tracker.report(new RuntimeException("something broke"), "log message");

        assertThat(receivedBodies).hasSize(1);
        String body = receivedBodies.get(0);
        assertThat(body).contains("\"environment\":\"production\"");
        assertThat(body).contains("\"exception_class\":\"java.lang.RuntimeException\"");
        assertThat(body).contains("\"message\":\"log message\"");
        assertThat(body).contains("\"stack_trace\":");
        assertThat(body).doesNotContain("\"release\":");
    }

    @Test
    void reportIncludesReleaseWhenConfigured() {
        ErrorTrackerConfiguration config = ErrorTrackerConfiguration.builder()
                .kendoUrl("http://localhost:" + mockPort)
                .projectId("proj1")
                .token("token123")
                .release("2.0.0")
                .build();
        new ErrorTracker(config).report(new RuntimeException(), "msg");

        assertThat(receivedBodies.get(0)).contains("\"release\":\"2.0.0\"");
    }

    @Test
    void reportWithNullThrowableUsesNoneAsExceptionClass() {
        new ErrorTracker(configForMock()).report(null, "warn message");

        String body = receivedBodies.get(0);
        assertThat(body).contains("\"exception_class\":\"none\"");
        assertThat(body).contains("\"stack_trace\":\"\"");
    }

    @Test
    void reportWithNullMessageSendsEmptyString() {
        new ErrorTracker(configForMock()).report(new RuntimeException(), null);

        assertThat(receivedBodies.get(0)).contains("\"message\":\"\"");
    }

    @Test
    void reportDoesNotThrowOnNon202Response() {
        responseCode = 500;
        assertThatCode(() -> new ErrorTracker(configForMock()).report(new RuntimeException(), "msg"))
                .doesNotThrowAnyException();
    }

    @Test
    void reportDoesNotThrowOnConnectionFailure() {
        ErrorTrackerConfiguration config = ErrorTrackerConfiguration.builder()
                .kendoUrl("http://localhost:1")
                .projectId("proj")
                .token("tok")
                .connectTimeoutSeconds(0.1)
                .timeoutSeconds(0.1)
                .build();
        assertThatCode(() -> new ErrorTracker(config).report(new RuntimeException(), "msg"))
                .doesNotThrowAnyException();
    }

    @Test
    void reportDoesNotThrowWhenSanitizerThrows() {
        List<Sanitizer> broken = List.of(input -> { throw new RuntimeException("sanitizer failure"); });
        ErrorTrackerConfiguration config = ErrorTrackerConfiguration.builder()
                .kendoUrl("http://localhost:" + mockPort)
                .projectId("proj")
                .token("tok")
                .sanitizers(broken)
                .build();
        assertThatCode(() -> new ErrorTracker(config).report(new RuntimeException(), "msg"))
                .doesNotThrowAnyException();
    }

    @Test
    void reportEscapesSpecialCharactersInMessage() {
        new ErrorTracker(configForMock()).report(null, "line1\nline2\r\ttab\"quote\\backslash");

        String body = receivedBodies.get(0);
        assertThat(body).contains("line1\\nline2\\r\\ttab\\\"quote\\\\backslash");
    }

    @Test
    void reportEscapesControlCharactersInMessage() {
        new ErrorTracker(configForMock()).report(null, "");

        String body = receivedBodies.get(0);
        assertThat(body).contains("\\u0001");
        assertThat(body).contains("\\u001f");
    }

    @Test
    void reportTruncatesLongMessage() {
        String longMessage = "x".repeat(70_000);
        new ErrorTracker(configForMock()).report(null, longMessage);

        String body = receivedBodies.get(0);
        assertThat(body).doesNotContain("x".repeat(65_536));
        assertThat(body).contains("x".repeat(65_535));
    }

    private ErrorTrackerConfiguration configForMock() {
        return ErrorTrackerConfiguration.builder()
                .kendoUrl("http://localhost:" + mockPort)
                .projectId("proj1")
                .token("token123")
                .build();
    }
}
