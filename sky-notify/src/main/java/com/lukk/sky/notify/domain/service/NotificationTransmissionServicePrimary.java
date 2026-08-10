package com.lukk.sky.notify.domain.service;

import com.google.gson.Gson;
import com.lukk.sky.common.kafka.KafkaPayloadModel;
import com.lukk.sky.notify.adapters.dto.WebsocketPayloadModel;
import com.lukk.sky.notify.adapters.outbound.NotificationPublisherPrimary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Translates a Kafka payload into a per-user WebSocket payload.
 *
 * <p>The Kafka producer side (booking/offer) stamps the originating user identity into
 * {@link KafkaPayloadModel#userInfo()}; we route the WebSocket message to that user only,
 * never broadcasting.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Primary
public class NotificationTransmissionServicePrimary implements NotificationTransmissionService {

    private static final Gson GSON = new Gson();

    private final NotificationPublisherPrimary notificationPublisherPrimary;

    @Override
    public void notifyClient(KafkaPayloadModel payload, String partition, String topic, String groupId, String timestamp, String offset) {
        if (payload == null || payload.userInfo() == null || payload.userInfo().isBlank()) {
            log.warn("Dropping Kafka message with missing userInfo (topic={}, offset={})", topic, offset);

            return;
        }

        String websocketPayloadJson = GSON.toJson(new WebsocketPayloadModel(
                payload, partition, topic, groupId, timestamp, offset));

        notificationPublisherPrimary.publish(payload.userInfo(), websocketPayloadJson);
        log.info("Notification routed to user='{}' (topic={}, offset={})",
                payload.userInfo(), topic, offset);
    }
}
