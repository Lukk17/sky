package com.lukk.sky.notify.adapters.inbound;

import com.lukk.sky.notify.domain.service.NotificationTransmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import static com.lukk.sky.notify.config.Constants.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaListeners {

    private final NotificationTransmissionService notificationTransmissionService;

    @KafkaListener(
            topics = KAFKA_OFFER_TOPIC,
            groupId = CONSUMER_GROUP_ID)
    void offerListener(@Payload String message,
                       @Header(KafkaHeaders.RECEIVED_PARTITION) String partition,
                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                       @Header(KafkaHeaders.GROUP_ID) String groupId,
                       @Header(KafkaHeaders.RECEIVED_TIMESTAMP) String timestamp,
                       @Header(KafkaHeaders.OFFSET) String offset) {

        log.info("Offer message arrived via Kafka");
        notificationTransmissionService.notifyClient(message, partition, topic, groupId, timestamp, offset);
    }

    @KafkaListener(
            topics = KAFKA_BOOKING_TOPIC,
            groupId = CONSUMER_GROUP_ID)
    void bookingListener(@Payload String message,
                         @Header(KafkaHeaders.RECEIVED_PARTITION) String partition,
                         @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                         @Header(KafkaHeaders.GROUP_ID) String groupId,
                         @Header(KafkaHeaders.RECEIVED_TIMESTAMP) String timestamp,
                         @Header(KafkaHeaders.OFFSET) String offset) {

        log.info("Booking message arrived via Kafka");
        notificationTransmissionService.notifyClient(message, partition, topic, groupId, timestamp, offset);
    }
}
