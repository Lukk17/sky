package com.lukk.sky.offer.domain.ports.outbound;

import com.lukk.sky.common.kafka.KafkaPayloadModel;

/**
 * This interface defines the contract for sending notifications related to offers.
 * The notification messages are constructed from the {@link KafkaPayloadModel}.
 */
public interface OfferNotificationService {
    /**
     * Sends a message constructed from the given {@link KafkaPayloadModel}.
     *
     * @param message the message to be sent
     */
    void sendMessage(KafkaPayloadModel message);
}
