package com.lukk.sky.booking.adapters.dto;

import java.util.List;

public record KafkaPayloadModel(String message, String accessedAt, List<String> userInfo) {

}
