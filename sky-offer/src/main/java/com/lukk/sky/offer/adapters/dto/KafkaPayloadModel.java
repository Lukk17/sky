package com.lukk.sky.offer.adapters.dto;

import java.time.LocalDateTime;
import java.util.List;

public record KafkaPayloadModel(String message, String accessedAt, List<String> userInfo) {

}
