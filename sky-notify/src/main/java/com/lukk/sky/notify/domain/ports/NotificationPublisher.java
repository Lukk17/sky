package com.lukk.sky.notify.domain.ports;

import com.lukk.sky.common.kafka.KafkaPayloadModel;

/**
 * Port for delivering a consumed event to the notification channel of a single user.
 */
public interface NotificationPublisher {

    void publish(String targetUser,
                 KafkaPayloadModel payload,
                 String partition,
                 String topic,
                 String groupId,
                 String timestamp,
                 String offset);
}
