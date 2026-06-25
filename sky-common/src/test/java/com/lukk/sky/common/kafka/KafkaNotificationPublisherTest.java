package com.lukk.sky.common.kafka;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("KafkaNotificationPublisher")
@ExtendWith(MockitoExtension.class)
class KafkaNotificationPublisherTest {

    private static final String TOPIC = "test-topic";

    @Mock
    KafkaTemplate<String, String> kafkaTemplate;

    KafkaNotificationPublisher publisher;

    private ListAppender<ILoggingEvent> logAppender;
    private Logger publisherLogger;

    @BeforeEach
    void setUp() {
        publisher = new KafkaNotificationPublisher(kafkaTemplate, TOPIC);

        publisherLogger = (Logger) LoggerFactory.getLogger(KafkaNotificationPublisher.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        publisherLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        publisherLogger.detachAppender(logAppender);
    }

    @Test
    @DisplayName("publish_sendsJsonToCorrectTopic_whenPayloadIsValid")
    void publish_sendsJsonToCorrectTopic_whenPayloadIsValid() {
        // Given
        KafkaPayloadModel payload = new KafkaPayloadModel("hello", "2024-01-01T00:00:00", "user@example.com");
        when(kafkaTemplate.send(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When
        publisher.publish(payload);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), messageCaptor.capture());

        assertThat(topicCaptor.getValue())
                .as("message must be sent to the configured topic")
                .isEqualTo(TOPIC);
        assertThat(messageCaptor.getValue())
                .as("message must be serialised to JSON containing the payload field")
                .contains("\"payload\":\"hello\"")
                .contains("\"userInfo\":\"user@example.com\"");
    }

    @Test
    @DisplayName("publish_isStateless_betweenCalls")
    void publish_isStateless_betweenCalls() {
        // Given
        KafkaPayloadModel first = new KafkaPayloadModel("first", "2024-01-01T00:00:00", "a@b.com");
        KafkaPayloadModel second = new KafkaPayloadModel("second", "2024-01-02T00:00:00", "c@d.com");
        when(kafkaTemplate.send(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When
        publisher.publish(first);
        publisher.publish(second);

        // Then
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(2)).send(anyString(), messageCaptor.capture());

        assertThat(messageCaptor.getAllValues().get(0)).contains("\"payload\":\"first\"");
        assertThat(messageCaptor.getAllValues().get(1)).contains("\"payload\":\"second\"");
    }

    @Test
    @DisplayName("publish_logsWarnAndDoesNotThrow_whenSendFutureFails")
    void publish_logsWarnAndDoesNotThrow_whenSendFutureFails() {
        // Given
        KafkaPayloadModel payload = new KafkaPayloadModel("data", "2024-01-01T00:00:00", "x@y.com");
        CompletableFuture<SendResult<String, String>> failed =
                CompletableFuture.failedFuture(new RuntimeException("broker unavailable"));
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(failed);

        // When
        assertThatCode(() -> publisher.publish(payload))
                .as("publish must not propagate the future failure to the caller")
                .doesNotThrowAnyException();

        // Then
        List<ILoggingEvent> warnEvents = logAppender.list.stream()
                .filter(e -> e.getLevel() == Level.WARN)
                .toList();

        assertThat(warnEvents)
                .as("exactly one WARN must be emitted on send failure")
                .hasSize(1);
        assertThat(warnEvents.get(0).getFormattedMessage())
                .as("WARN message must reference the topic")
                .contains(TOPIC);
    }
}
