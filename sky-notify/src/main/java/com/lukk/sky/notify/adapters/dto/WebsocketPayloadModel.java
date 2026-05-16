package com.lukk.sky.notify.adapters.dto;

import com.lukk.sky.common.kafka.KafkaPayloadModel;

public record WebsocketPayloadModel(KafkaPayloadModel kafkaPayloadModel, String partition,
                                    String topic, String groupId, String timestamp, String offset) {
}
