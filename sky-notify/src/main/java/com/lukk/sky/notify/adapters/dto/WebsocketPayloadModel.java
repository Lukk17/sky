package com.lukk.sky.notify.adapters.dto;

public record WebsocketPayloadModel(KafkaPayloadModel kafkaPayloadModel, String partition,
                                    String topic, String groupId, String timestamp, String offset) {
}
