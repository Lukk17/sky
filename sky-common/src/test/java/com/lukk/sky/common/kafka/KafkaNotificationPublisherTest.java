package com.lukk.sky.common.kafka;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.gson.Gson;
import com.lukk.sky.common.web.CorrelationId;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("KafkaNotificationPublisher")
@ExtendWith(MockitoExtension.class)
class KafkaNotificationPublisherTest {

    private static final String TOPIC = "test-topic";

    @Mock
    KafkaTemplate<String, String> kafkaTemplate;

    @Captor
    ArgumentCaptor<ProducerRecord<String, String>> recordCaptor;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    KafkaNotificationPublisher publisher;

    private ListAppender<ILoggingEvent> logAppender;
    private Logger publisherLogger;

    @BeforeEach
    void setUp() {
        publisher = new KafkaNotificationPublisher(kafkaTemplate, objectMapper, TOPIC);

        publisherLogger = (Logger) LoggerFactory.getLogger(KafkaNotificationPublisher.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        publisherLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        publisherLogger.detachAppender(logAppender);
        CorrelationId.clear();
    }

    @Test
    @DisplayName("publish_sendsJsonToCorrectTopic_whenPayloadIsValid")
    void publish_sendsJsonToCorrectTopic_whenPayloadIsValid() {
        // given
        KafkaPayloadModel payload = new KafkaPayloadModel("hello", "2024-01-01T00:00:00", "user@example.com");
        givenSendSucceeds();

        // when
        publisher.publish(payload);

        // then
        verify(kafkaTemplate).send(recordCaptor.capture());

        assertThat(recordCaptor.getValue().topic())
                .as("message must be sent to the configured topic")
                .isEqualTo(TOPIC);
        assertThat(recordCaptor.getValue().value())
                .as("message must be serialised to JSON containing the payload field")
                .contains("\"payload\":\"hello\"")
                .contains("\"userInfo\":\"user@example.com\"");
    }

    @Test
    @DisplayName("publish_producesJsonIdenticalToGson_whenPayloadIsFullyPopulated")
    void publish_producesJsonIdenticalToGson_whenPayloadIsFullyPopulated() {
        // given
        KafkaPayloadModel payload = new KafkaPayloadModel("hello", "2024-01-01T00:00:00", "user@example.com");
        givenSendSucceeds();

        // when
        publisher.publish(payload);

        // then
        verify(kafkaTemplate).send(recordCaptor.capture());

        assertThat(recordCaptor.getValue().value())
                .as("the Jackson wire format must stay byte-identical to the Gson format consumers still expect")
                .isEqualTo(new Gson().toJson(payload));
    }

    @Test
    @DisplayName("publish_omitsNullFields_soTheWireFormatMatchesGson")
    void publish_omitsNullFields_soTheWireFormatMatchesGson() {
        // given
        KafkaPayloadModel payload = new KafkaPayloadModel("hello", null, null);
        givenSendSucceeds();

        // when
        publisher.publish(payload);

        // then
        verify(kafkaTemplate).send(recordCaptor.capture());

        assertThat(recordCaptor.getValue().value())
                .as("null components must be omitted exactly as Gson omits them")
                .isEqualTo(new Gson().toJson(payload))
                .isEqualTo("{\"payload\":\"hello\"}");
    }

    @Test
    @DisplayName("publish_addsCorrelationIdHeader_whenTheRequestCarriesOne")
    void publish_addsCorrelationIdHeader_whenTheRequestCarriesOne() {
        // given
        CorrelationId.set("correlation-42");
        givenSendSucceeds();

        // when
        publisher.publish(new KafkaPayloadModel("hello", "now", "user@example.com"));

        // then
        verify(kafkaTemplate).send(recordCaptor.capture());

        Header header = recordCaptor.getValue().headers().lastHeader(CorrelationId.HEADER);

        assertThat(header)
                .as("the correlation ID must travel with the event")
                .isNotNull();
        assertThat(new String(header.value(), StandardCharsets.UTF_8))
                .isEqualTo("correlation-42");
    }

    @Test
    @DisplayName("publish_addsNoCorrelationIdHeader_whenNoneIsInScope")
    void publish_addsNoCorrelationIdHeader_whenNoneIsInScope() {
        // given
        givenSendSucceeds();

        // when
        publisher.publish(new KafkaPayloadModel("hello", "now", "user@example.com"));

        // then
        verify(kafkaTemplate).send(recordCaptor.capture());

        assertThat(recordCaptor.getValue().headers().lastHeader(CorrelationId.HEADER))
                .as("no correlation ID in scope must leave the record header absent")
                .isNull();
    }

    @Test
    @DisplayName("publish_isStateless_betweenCalls")
    void publish_isStateless_betweenCalls() {
        // given
        KafkaPayloadModel first = new KafkaPayloadModel("first", "2024-01-01T00:00:00", "a@b.com");
        KafkaPayloadModel second = new KafkaPayloadModel("second", "2024-01-02T00:00:00", "c@d.com");
        givenSendSucceeds();

        // when
        publisher.publish(first);
        publisher.publish(second);

        // then
        verify(kafkaTemplate, times(2)).send(recordCaptor.capture());

        assertThat(recordCaptor.getAllValues().get(0).value()).contains("\"payload\":\"first\"");
        assertThat(recordCaptor.getAllValues().get(1).value()).contains("\"payload\":\"second\"");
    }

    @Test
    @DisplayName("publish_logsWarnAndDoesNotThrow_whenSendFutureFails")
    void publish_logsWarnAndDoesNotThrow_whenSendFutureFails() {
        // given
        KafkaPayloadModel payload = new KafkaPayloadModel("data", "2024-01-01T00:00:00", "x@y.com");
        CompletableFuture<SendResult<String, String>> failed =
                CompletableFuture.failedFuture(new RuntimeException("broker unavailable"));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(failed);

        // when / then
        assertThatCode(() -> publisher.publish(payload))
                .as("publish must not propagate the future failure to the caller")
                .doesNotThrowAnyException();

        List<ILoggingEvent> warnEvents = logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .toList();

        assertThat(warnEvents)
                .as("exactly one WARN must be emitted on send failure")
                .hasSize(1);
        assertThat(warnEvents.get(0).getFormattedMessage())
                .as("WARN message must reference the topic")
                .contains(TOPIC);
    }

    private void givenSendSucceeds() {
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
    }
}
