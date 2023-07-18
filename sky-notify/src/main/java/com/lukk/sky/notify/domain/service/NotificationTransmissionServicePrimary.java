package com.lukk.sky.notify.domain.service;

import com.google.gson.Gson;
import com.lukk.sky.notify.adapters.dto.KafkaPayloadModel;
import com.lukk.sky.notify.adapters.dto.WebsocketPayloadModel;
import com.lukk.sky.notify.adapters.outbound.NotificationPublisherPrimary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * The primary implementation of the {@link NotificationTransmissionService} interface.
 * This implementation uses a {@link NotificationPublisherPrimary} to publish notifications.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Primary
public class NotificationTransmissionServicePrimary implements NotificationTransmissionService {

    private final NotificationPublisherPrimary notificationPublisherPrimary;

    /**
     * {@inheritDoc}
     * <p>
     * This implementation deserializes the message from JSON into a {@link KafkaPayloadModel} object.
     * Then it creates a new {@link WebsocketPayloadModel} object, with the KafkaPayloadModel and the
     * other provided parameters, serializes it into JSON and sends it to a client through a WebSocket.
     * Finally, it logs the sent notification.
     */
    @Override
    public void notifyClient(String message, String partition, String topic, String groupId, String timestamp, String offset) {
        Gson gson = new Gson();

        KafkaPayloadModel payloadModel = gson.fromJson(message, KafkaPayloadModel.class);

        String websocketPayloadJson = gson.toJson(new WebsocketPayloadModel(
                payloadModel, partition, topic, groupId, timestamp, offset));

        notificationPublisherPrimary.publish(websocketPayloadJson);
        log.info("Notification sent to websocket with payload: {}", websocketPayloadJson);
    }
}
