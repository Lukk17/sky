package com.lukk.sky.notify.adapters.inbound;

import com.google.gson.Gson;
import com.lukk.sky.common.kafka.KafkaPayloadModel;
import com.lukk.sky.notify.domain.service.NotificationTransmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import static com.lukk.sky.notify.config.Constants.CONSUMER_GROUP_ID;
import static com.lukk.sky.notify.config.Constants.KAFKA_BOOKING_TOPIC;
import static com.lukk.sky.notify.config.Constants.KAFKA_OFFER_TOPIC;

/**
 * Kafka inbound adapters for sky-notify. Each listener delegates to
 * {@link NotificationTransmissionService} and acks only after the side-effect
 * succeeds. Unhandled exceptions bubble to the container's DefaultErrorHandler,
 * which retries via FixedBackOff and ultimately routes to {@code <topic>.DLT}
 * (see {@code KafkaConsumerConfig}).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaListeners {

    private static final Gson GSON = new Gson();

    private final NotificationTransmissionService notificationTransmissionService;

    @KafkaListener(topics = KAFKA_OFFER_TOPIC, groupId = CONSUMER_GROUP_ID)
    void offerListener(@Payload String message,
                       @Header(KafkaHeaders.RECEIVED_PARTITION) String partition,
                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                       @Header(KafkaHeaders.GROUP_ID) String groupId,
                       @Header(KafkaHeaders.RECEIVED_TIMESTAMP) String timestamp,
                       @Header(KafkaHeaders.OFFSET) String offset,
                       Acknowledgment ack) {
        log.info("Offer message arrived via Kafka (topic={}, offset={})", topic, offset);
        KafkaPayloadModel payload = GSON.fromJson(message, KafkaPayloadModel.class);
        notificationTransmissionService.notifyClient(payload, partition, topic, groupId, timestamp, offset);
        ack.acknowledge();
    }

    @KafkaListener(topics = KAFKA_BOOKING_TOPIC, groupId = CONSUMER_GROUP_ID)
    void bookingListener(@Payload String message,
                         @Header(KafkaHeaders.RECEIVED_PARTITION) String partition,
                         @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                         @Header(KafkaHeaders.GROUP_ID) String groupId,
                         @Header(KafkaHeaders.RECEIVED_TIMESTAMP) String timestamp,
                         @Header(KafkaHeaders.OFFSET) String offset,
                         Acknowledgment ack) {
        log.info("Booking message arrived via Kafka (topic={}, offset={})", topic, offset);
        KafkaPayloadModel payload = GSON.fromJson(message, KafkaPayloadModel.class);
        notificationTransmissionService.notifyClient(payload, partition, topic, groupId, timestamp, offset);
        ack.acknowledge();
    }
}
