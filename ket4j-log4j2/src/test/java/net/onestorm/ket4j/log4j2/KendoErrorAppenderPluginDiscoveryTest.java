package net.onestorm.ket4j.log4j2;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class KendoErrorAppenderPluginDiscoveryTest {

    @Test
    void kendoErrorAppenderIsDiscoverableViaXmlConfig() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Configuration status="WARN">
                    <Appenders>
                        <KendoError name="Kendo"/>
                    </Appenders>
                    <Loggers>
                        <Root level="warn">
                            <AppenderRef ref="Kendo"/>
                        </Root>
                    </Loggers>
                </Configuration>
                """;
        ConfigurationSource source = new ConfigurationSource(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        LoggerContext context = Configurator.initialize(null, source);
        try {
            assertThat(context.getConfiguration().getAppenders()).containsKey("Kendo");
        } finally {
            Configurator.shutdown(context);
        }
    }
}
