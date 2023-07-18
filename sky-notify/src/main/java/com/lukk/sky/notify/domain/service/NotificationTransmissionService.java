package com.lukk.sky.notify.domain.service;

/**
 * Service for transmitting notifications to a client.
 */
public interface NotificationTransmissionService {

    /**
     * Notify a client with a given message and associated metadata.
     *
     * @param message   The actual message to be sent. Must not be {@code null}.
     * @param partition The partition from where the message originated.
     * @param topic     The topic from where the message originated.
     * @param groupId   The group id of the consumer.
     * @param timestamp The timestamp of the message.
     * @param offset    The offset of the message in its partition.
     * @throws IllegalArgumentException if {@code message} is {@code null}.
     */
    void notifyClient(String message, String partition, String topic, String groupId, String timestamp, String offset);
}
