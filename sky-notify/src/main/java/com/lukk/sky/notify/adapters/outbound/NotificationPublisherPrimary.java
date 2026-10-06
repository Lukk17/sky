package com.lukk.sky.notify.adapters.outbound;

import com.lukk.sky.common.kafka.KafkaPayloadModel;
import com.lukk.sky.notify.adapters.dto.WebsocketPayloadModel;
import com.lukk.sky.notify.adapters.outbound.websocket.WebSocketService;
import com.lukk.sky.notify.domain.ports.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Primary
public class NotificationPublisherPrimary implements NotificationPublisher {

    private final WebSocketService webSocketService;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(String targetUser,
                        KafkaPayloadModel payload,
                        String partition,
                        String topic,
                        String groupId,
                        String timestamp,
                        String offset) {

        WebsocketPayloadModel websocketPayload =
                new WebsocketPayloadModel(payload, partition, topic, groupId, timestamp, offset);

        webSocketService.triggerMessage(targetUser, objectMapper.writeValueAsString(websocketPayload));
    }
}
