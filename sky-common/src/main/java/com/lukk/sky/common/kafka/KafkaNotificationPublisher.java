package com.lukk.sky.common.kafka;

import com.lukk.sky.common.web.CorrelationId;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@Slf4j
public final class KafkaNotificationPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaNotificationPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            String topic) {

        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    public void publish(KafkaPayloadModel message) {
        ProducerRecord<String, String> record =
                new ProducerRecord<>(topic, objectMapper.writeValueAsString(message));

        CorrelationId.current().ifPresent(correlationId ->
                record.headers().add(CorrelationId.HEADER, correlationId.getBytes(StandardCharsets.UTF_8)));

        kafkaTemplate.send(record)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("kafka.send.failed topic={}", topic, ex);
                    }
                });
    }
}
