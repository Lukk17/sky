package com.lukk.sky.notify.adapters.dto;

public record KafkaPayloadModel(String message, String accessedAt, String userInfo) {

}
