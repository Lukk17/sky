package com.lukk.sky.common.kafka;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record KafkaPayloadModel(String payload, String accessedAt, String userInfo) {
}
