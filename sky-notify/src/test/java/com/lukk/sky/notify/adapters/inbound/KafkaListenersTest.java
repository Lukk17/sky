package com.lukk.sky.notify.adapters.inbound;

import com.lukk.sky.common.kafka.KafkaPayloadModel;
import com.lukk.sky.common.web.CorrelationId;
import com.lukk.sky.notify.domain.service.NotificationTransmissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.kafka.support.Acknowledgment;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static com.lukk.sky.notify.config.Constants.KAFKA_BOOKING_TOPIC;
import static com.lukk.sky.notify.config.Constants.KAFKA_OFFER_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("KafkaListeners: inbound Kafka adapter")
@ExtendWith(MockitoExtension.class)
class KafkaListenersTest {

    private static final String TEST_PARTITION = "1";
    private static final String TEST_OFFSET = "0";
    private static final String TEST_CONSUMER_GROUP_ID = "first-consumer-group";
    private static final String TEST_CORRELATION_ID = "corr-9f2c";
    private static final LocalDateTime TEST_DATE = LocalDateTime.of(2201, 6, 20, 16, 35, 47);

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Mock
    private NotificationTransmissionService notificationTransmissionService;

    @Mock
    private Acknowledgment acknowledgment;

    private KafkaListeners kafkaListeners;

    @BeforeEach
    void createListeners() {
        kafkaListeners = new KafkaListeners(notificationTransmissionService, objectMapper);
    }

    @AfterEach
    void clearMdc() {
        CorrelationId.clear();
    }

    @Test
    @DisplayName("offerListener deserializes the payload with Jackson and acknowledges it")
    void offerListener_whenNotifySucceeds_thenDeserializesAndAcknowledges() {
        // given
        KafkaPayloadModel payload = new KafkaPayloadModel("offer-data", TEST_DATE.toString(), "user@test.com");

        // when
        kafkaListeners.offerListener(objectMapper.writeValueAsString(payload), TEST_PARTITION, KAFKA_OFFER_TOPIC,
                TEST_CONSUMER_GROUP_ID, TEST_DATE.toString(), TEST_OFFSET, null, acknowledgment);

        // then
        verify(notificationTransmissionService).notifyClient(payload, TEST_PARTITION, KAFKA_OFFER_TOPIC,
                TEST_CONSUMER_GROUP_ID, TEST_DATE.toString(), TEST_OFFSET);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("bookingListener deserializes the payload with Jackson and acknowledges it")
    void bookingListener_whenNotifySucceeds_thenDeserializesAndAcknowledges() {
        // given
        KafkaPayloadModel payload = new KafkaPayloadModel("booking-data", TEST_DATE.toString(), "user@test.com");

        // when
        kafkaListeners.bookingListener(objectMapper.writeValueAsString(payload), TEST_PARTITION, KAFKA_BOOKING_TOPIC,
                TEST_CONSUMER_GROUP_ID, TEST_DATE.toString(), TEST_OFFSET, null, acknowledgment);

        // then
        verify(notificationTransmissionService).notifyClient(payload, TEST_PARTITION, KAFKA_BOOKING_TOPIC,
                TEST_CONSUMER_GROUP_ID, TEST_DATE.toString(), TEST_OFFSET);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("offerListener does not acknowledge when notifyClient throws")
    void offerListener_whenNotifyThrows_thenDoesNotAcknowledge() {
        // given
        KafkaPayloadModel payload = new KafkaPayloadModel("boom", TEST_DATE.toString(), "user@test.com");
        String message = objectMapper.writeValueAsString(payload);
        doThrow(new RuntimeException("ws failed"))
                .when(notificationTransmissionService)
                .notifyClient(payload, TEST_PARTITION, KAFKA_OFFER_TOPIC,
                        TEST_CONSUMER_GROUP_ID, TEST_DATE.toString(), TEST_OFFSET);

        // when / then
        assertThatThrownBy(() -> kafkaListeners.offerListener(message, TEST_PARTITION, KAFKA_OFFER_TOPIC,
                TEST_CONSUMER_GROUP_ID, TEST_DATE.toString(), TEST_OFFSET, null, acknowledgment))
                .isInstanceOf(RuntimeException.class);

        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    @DisplayName("malformed JSON fails before acknowledgement so the error handler can dead-letter it")
    void offerListener_whenMessageIsMalformed_thenFailsWithoutAcknowledging() {
        // when / then
        assertThatThrownBy(() -> kafkaListeners.offerListener("{not-json", TEST_PARTITION, KAFKA_OFFER_TOPIC,
                TEST_CONSUMER_GROUP_ID, TEST_DATE.toString(), TEST_OFFSET, null, acknowledgment))
                .isInstanceOf(JacksonException.class);

        verify(acknowledgment, never()).acknowledge();
        verify(notificationTransmissionService, never())
                .notifyClient(any(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("carries the record correlation id while handling and clears it afterwards")
    void offerListener_whenRecordCarriesCorrelationHeader_thenCorrelationIdIsSetWhileHandlingAndClearedAfter() {
        // given
        KafkaPayloadModel payload = new KafkaPayloadModel("offer-data", TEST_DATE.toString(), "user@test.com");
        AtomicReference<String> observed = new AtomicReference<>();
        doAnswer(invocation -> {
            observed.set(MDC.get(CorrelationId.MDC_KEY));

            return null;
        }).when(notificationTransmissionService)
                .notifyClient(any(), anyString(), anyString(), anyString(), anyString(), anyString());

        // when
        kafkaListeners.offerListener(objectMapper.writeValueAsString(payload), TEST_PARTITION, KAFKA_OFFER_TOPIC,
                TEST_CONSUMER_GROUP_ID, TEST_DATE.toString(), TEST_OFFSET,
                TEST_CORRELATION_ID.getBytes(StandardCharsets.UTF_8), acknowledgment);

        // then
        assertThat(observed.get()).isEqualTo(TEST_CORRELATION_ID);
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("clears the correlation id even when handling fails")
    void offerListener_whenNotifyThrows_thenCorrelationIdIsStillCleared() {
        // given
        KafkaPayloadModel payload = new KafkaPayloadModel("boom", TEST_DATE.toString(), "user@test.com");
        String message = objectMapper.writeValueAsString(payload);
        doThrow(new RuntimeException("ws failed"))
                .when(notificationTransmissionService)
                .notifyClient(any(), anyString(), anyString(), anyString(), anyString(), anyString());

        // when / then
        assertThatThrownBy(() -> kafkaListeners.offerListener(message, TEST_PARTITION, KAFKA_OFFER_TOPIC,
                TEST_CONSUMER_GROUP_ID, TEST_DATE.toString(), TEST_OFFSET,
                TEST_CORRELATION_ID.getBytes(StandardCharsets.UTF_8), acknowledgment))
                .isInstanceOf(RuntimeException.class);

        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("leaves no correlation id behind when the record carries no header")
    void offerListener_whenRecordHasNoCorrelationHeader_thenNoCorrelationIdIsSet() {
        // given
        KafkaPayloadModel payload = new KafkaPayloadModel("offer-data", TEST_DATE.toString(), "user@test.com");
        AtomicReference<String> observed = new AtomicReference<>("initial");
        doAnswer(invocation -> {
            observed.set(MDC.get(CorrelationId.MDC_KEY));

            return null;
        }).when(notificationTransmissionService)
                .notifyClient(any(), anyString(), anyString(), anyString(), anyString(), anyString());

        // when
        kafkaListeners.offerListener(objectMapper.writeValueAsString(payload), TEST_PARTITION, KAFKA_OFFER_TOPIC,
                TEST_CONSUMER_GROUP_ID, TEST_DATE.toString(), TEST_OFFSET, new byte[0], acknowledgment);

        // then
        assertThat(observed.get()).isNull();
    }
}
