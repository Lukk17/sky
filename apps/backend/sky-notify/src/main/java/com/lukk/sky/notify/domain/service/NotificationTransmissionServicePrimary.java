package com.lukk.sky.notify.domain.service;

import com.lukk.sky.common.kafka.KafkaPayloadModel;
import com.lukk.sky.notify.domain.ports.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Routes a consumed event to the user that triggered it.
 *
 * <p>The producing service (booking/offer) stamps the originating user identity into
 * {@link KafkaPayloadModel#userInfo()}; the event goes to that user only, never broadcast.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Primary
public class NotificationTransmissionServicePrimary implements NotificationTransmissionService {

    private final NotificationPublisher notificationPublisher;

    @Override
    public void notifyClient(KafkaPayloadModel payload, String partition, String topic, String groupId, String timestamp, String offset) {
        if (payload == null || payload.userInfo() == null || payload.userInfo().isBlank()) {
            log.warn("notify.dropped reason=missing_user_info topic={} offset={}", topic, offset);

            return;
        }

        notificationPublisher.publish(payload.userInfo(), payload, partition, topic, groupId, timestamp, offset);
        log.info("notify.routed user={} topic={} offset={}", payload.userInfo(), topic, offset);
    }
}
