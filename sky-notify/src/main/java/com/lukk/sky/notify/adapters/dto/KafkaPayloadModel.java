package com.lukk.sky.notify.adapters.dto;

import java.util.List;

public record KafkaPayloadModel(String message, String accessedAt, List<String> userInfo) {

}
