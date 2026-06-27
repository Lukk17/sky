package com.lukk.sky.booking.domain.ports.outbound;


import com.lukk.sky.common.kafka.KafkaPayloadModel;

/**
 * Service interface for sending booking notifications.
 * This service provides operations for sending messages.
 */
public interface BookingNotificationService {
    /**
     * Sends a message.
     *
     * @param message The message to be sent.
     */
    void sendMessage(KafkaPayloadModel message);
}
