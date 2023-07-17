package com.lukk.sky.notify.domain.service;

import com.google.gson.Gson;
import com.lukk.sky.notify.adapters.dto.KafkaPayloadModel;
import com.lukk.sky.notify.adapters.dto.WebsocketPayloadModel;
import com.lukk.sky.notify.adapters.outbound.NotificationPublisherPrimary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@Primary
public class NotificationTransmissionServicePrimary implements NotificationTransmissionService {

    private final NotificationPublisherPrimary notificationPublisherPrimary;

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
