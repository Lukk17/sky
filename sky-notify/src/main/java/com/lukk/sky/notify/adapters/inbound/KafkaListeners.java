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

/**
 * Class defining listeners for Kafka topics.
 * It listens to the specified Kafka topics and when a message arrives, it uses the
 * {@link NotificationTransmissionService} to notify the client about the message.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaListeners {

    private final NotificationTransmissionService notificationTransmissionService;

    /**
     * Listener for the Kafka topic for offers.
     * When a message arrives, it notifies the client with the message and its metadata.
     *
     * @param message   The received Kafka message.
     * @param partition The partition from which the message was received.
     * @param topic     The topic from which the message was received.
     * @param groupId   The group id of the consumer that received the message.
     * @param timestamp The timestamp when the message was received.
     * @param offset    The offset of the message in the topic.
     */
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

    /**
     * Listener for the Kafka topic for bookings.
     * When a message arrives, it notifies the client with the message and its metadata.
     *
     * @param message   The received Kafka message.
     * @param partition The partition from which the message was received.
     * @param topic     The topic from which the message was received.
     * @param groupId   The group id of the consumer that received the message.
     * @param timestamp The timestamp when the message was received.
     * @param offset    The offset of the message in the topic.
     */
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
