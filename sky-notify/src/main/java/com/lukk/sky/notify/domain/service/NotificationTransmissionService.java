package com.lukk.sky.notify.domain.service;

import com.lukk.sky.common.kafka.KafkaPayloadModel;

/**
 * Service for transmitting notifications to a client.
 */
public interface NotificationTransmissionService {

    /**
     * Notify a client with a typed Kafka payload and associated metadata.
     *
     * @param payload   The deserialized Kafka payload. Must not be {@code null}.
     * @param partition The partition from where the message originated.
     * @param topic     The topic from where the message originated.
     * @param groupId   The group id of the consumer.
     * @param timestamp The timestamp of the message.
     * @param offset    The offset of the message in its partition.
     */
    void notifyClient(KafkaPayloadModel payload, String partition, String topic, String groupId, String timestamp, String offset);
}
