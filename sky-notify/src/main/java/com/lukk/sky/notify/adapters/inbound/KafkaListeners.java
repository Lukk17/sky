package com.lukk.sky.notify.adapters.inbound;

import com.lukk.sky.common.kafka.KafkaPayloadModel;
import com.lukk.sky.common.web.CorrelationId;
import com.lukk.sky.notify.domain.service.NotificationTransmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import static com.lukk.sky.notify.config.Constants.CONSUMER_GROUP_ID;
import static com.lukk.sky.notify.config.Constants.KAFKA_BOOKING_TOPIC;
import static com.lukk.sky.notify.config.Constants.KAFKA_OFFER_TOPIC;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaListeners {

    private final NotificationTransmissionService notificationTransmissionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KAFKA_OFFER_TOPIC, groupId = CONSUMER_GROUP_ID)
    void offerListener(@Payload String message,
                       @Header(KafkaHeaders.RECEIVED_PARTITION) String partition,
                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                       @Header(KafkaHeaders.GROUP_ID) String groupId,
                       @Header(KafkaHeaders.RECEIVED_TIMESTAMP) String timestamp,
                       @Header(KafkaHeaders.OFFSET) String offset,
                       @Header(name = CorrelationId.HEADER, required = false) byte[] correlationId,
                       Acknowledgment ack) {
        consume(message, partition, topic, groupId, timestamp, offset, correlationId, ack);
    }

    @KafkaListener(topics = KAFKA_BOOKING_TOPIC, groupId = CONSUMER_GROUP_ID)
    void bookingListener(@Payload String message,
                         @Header(KafkaHeaders.RECEIVED_PARTITION) String partition,
                         @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                         @Header(KafkaHeaders.GROUP_ID) String groupId,
                         @Header(KafkaHeaders.RECEIVED_TIMESTAMP) String timestamp,
                         @Header(KafkaHeaders.OFFSET) String offset,
                         @Header(name = CorrelationId.HEADER, required = false) byte[] correlationId,
                         Acknowledgment ack) {
        consume(message, partition, topic, groupId, timestamp, offset, correlationId, ack);
    }

    private void consume(String message, String partition, String topic, String groupId,
                         String timestamp, String offset, byte[] correlationId, Acknowledgment ack) {
        adoptCorrelationId(correlationId);

        try {
            log.info("kafka.message.received topic={} offset={}", topic, offset);
            KafkaPayloadModel payload = objectMapper.readValue(message, KafkaPayloadModel.class);
            notificationTransmissionService.notifyClient(payload, partition, topic, groupId, timestamp, offset);

            ack.acknowledge();
        } finally {
            CorrelationId.clear();
        }
    }

    private static void adoptCorrelationId(byte[] correlationId) {
        if (correlationId == null || correlationId.length == 0) {
            return;
        }

        CorrelationId.set(new String(correlationId, StandardCharsets.UTF_8));
    }
}
