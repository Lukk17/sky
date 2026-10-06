package com.lukk.sky.notify.adapters.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lukk.sky.common.kafka.KafkaPayloadModel;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WebsocketPayloadModel(KafkaPayloadModel kafkaPayloadModel, String partition,
                                    String topic, String groupId, String timestamp, String offset) {
}
