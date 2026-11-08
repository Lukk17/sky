package com.lukk.sky.notify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.lukk.sky.common.web.CorrelationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Logging configuration: correlation id rendering")
class LoggingConfigurationTest {

    private static final Pattern ENCODER_PATTERN = Pattern.compile("<pattern>(.*?)</pattern>", Pattern.DOTALL);

    @Test
    @DisplayName("the shipped console pattern renders the correlation id held in the logging context")
    void consolePattern_whenCorrelationIdIsInTheLoggingContext_thenItIsRenderedOnTheLine() throws IOException {
        // given
        LoggerContext loggerContext = new LoggerContext();
        loggerContext.start();
        PatternLayout layout = new PatternLayout();
        layout.setContext(loggerContext);
        layout.setPattern(shippedConsolePattern());
        layout.start();

        LoggingEvent event = new LoggingEvent();
        event.setLoggerName("com.lukk.sky.notify.adapters.inbound.KafkaListeners");
        event.setLevel(Level.INFO);
        event.setMessage("kafka.message.received topic=sky.offer offset=1");
        event.setTimeStamp(System.currentTimeMillis());
        event.setMDCPropertyMap(Map.of(CorrelationId.MDC_KEY, "corr-abc123"));

        // when
        String rendered = layout.doLayout(event);

        // then
        assertThat(rendered).contains("corr-abc123");
    }

    private String shippedConsolePattern() throws IOException {
        try (InputStream configuration = getClass().getResourceAsStream("/logback-spring.xml")) {
            String xml = new String(requireNonNull(configuration).readAllBytes(), StandardCharsets.UTF_8);
            Matcher encoderPattern = ENCODER_PATTERN.matcher(xml);
            assertThat(encoderPattern.find()).isTrue();

            return encoderPattern.group(1).trim();
        }
    }
}
