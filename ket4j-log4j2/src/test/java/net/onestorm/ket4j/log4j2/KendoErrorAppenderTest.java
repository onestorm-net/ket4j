package net.onestorm.ket4j.log4j2;

import com.sun.net.httpserver.HttpServer;
import net.onestorm.ket4j.ErrorTrackerConfiguration;
import net.onestorm.ket4j.ErrorTrackerProvider;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class KendoErrorAppenderTest {

    private HttpServer mockServer;
    private int mockPort;
    private final List<String> receivedBodies = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        resetProvider();
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        mockPort = mockServer.getAddress().getPort();
        mockServer.createContext("/", exchange -> {
            byte[] bytes = exchange.getRequestBody().readAllBytes();
            receivedBodies.add(new String(bytes, StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(202, -1);
            exchange.close();
        });
        mockServer.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        mockServer.stop(0);
        receivedBodies.clear();
        resetProvider();
    }

    @Test
    void createAppenderReturnsNullWhenNameIsNull() {
        assertThat(KendoErrorAppender.createAppender(null, null, null, false)).isNull();
    }

    @Test
    void createAppenderReturnsInstanceWhenNameProvided() {
        assertThat(KendoErrorAppender.createAppender("test", null, null, false))
                .isInstanceOf(KendoErrorAppender.class);
    }

    @Test
    void appendReportsBelowWarnEventWhenNoFilterConfigured() {
        // No level filter is hardcoded — without a Filter attached, level doesn't restrict anything.
        initializeProvider();
        KendoErrorAppender appender = KendoErrorAppender.createAppender("test", null, null, false);
        LogEvent event = buildEvent(Level.INFO, "info message", new RuntimeException("info cause"));
        appender.append(event);
        assertThat(receivedBodies).hasSize(1);
    }

    @Test
    void appendHonorsConfiguredThresholdFilter() {
        initializeProvider();
        Filter filter = ThresholdFilter.createFilter(Level.WARN, Filter.Result.NEUTRAL, Filter.Result.DENY);
        KendoErrorAppender appender = KendoErrorAppender.createAppender("test", filter, null, false);

        appender.append(buildEvent(Level.INFO, "info message", new RuntimeException("info cause")));
        assertThat(receivedBodies).isEmpty();

        appender.append(buildEvent(Level.WARN, "warn message", new RuntimeException("warn cause")));
        assertThat(receivedBodies).hasSize(1);
    }

    @Test
    void appendSkipsWarnEventWithoutThrowable() {
        initializeProvider();
        KendoErrorAppender appender = KendoErrorAppender.createAppender("test", null, null, false);
        LogEvent event = buildEvent(Level.WARN, "no exception here", null);
        appender.append(event);
        assertThat(receivedBodies).isEmpty();
    }

    @Test
    void appendReportsWarnEvent() {
        initializeProvider();
        KendoErrorAppender appender = KendoErrorAppender.createAppender("test", null, null, false);
        LogEvent event = buildEvent(Level.WARN, "something degraded", new RuntimeException("warn cause"));
        appender.append(event);
        assertThat(receivedBodies).hasSize(1);
        assertThat(receivedBodies.get(0)).contains("\"message\":\"something degraded\"");
    }

    @Test
    void appendDoesNotThrowWhenProviderNotInitialized() {
        // Provider is intentionally not initialized — getInstance() throws IllegalStateException,
        // which the appender's catch block must swallow.
        KendoErrorAppender appender = KendoErrorAppender.createAppender("test", null, null, false);
        LogEvent event = buildEvent(Level.ERROR, "error", new RuntimeException("boom"));
        assertThatCode(() -> appender.append(event)).doesNotThrowAnyException();
    }

    private void initializeProvider() {
        ErrorTrackerConfiguration config = ErrorTrackerConfiguration.builder()
                .kendoUrl("http://localhost:" + mockPort)
                .projectId("proj1")
                .token("token123")
                .build();
        ErrorTrackerProvider.initialize(config);
    }

    private void resetProvider() throws Exception {
        Field field = ErrorTrackerProvider.class.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        AtomicReference<?> ref = (AtomicReference<?>) field.get(null);
        ref.set(null);
    }

    private LogEvent buildEvent(Level level, String message, Throwable throwable) {
        return Log4jLogEvent.newBuilder()
                .setLevel(level)
                .setMessage(new SimpleMessage(message))
                .setThrown(throwable)
                .build();
    }
}
