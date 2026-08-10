package com.lukk.sky.common.kafka;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
public final class KafkaNotificationPublisher {

    private static final Gson GSON = new Gson();

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public KafkaNotificationPublisher(KafkaTemplate<String, String> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(KafkaPayloadModel message) {
        kafkaTemplate.send(topic, GSON.toJson(message))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("kafka.send.failed topic={} key={}", topic, null, ex);
                    }
                });
    }
}
