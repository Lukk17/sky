package com.lukk.sky.common.kafka;

import com.google.gson.Gson;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Reusable helper that serialises a {@link KafkaPayloadModel} to JSON and
 * publishes it to a single Kafka topic.
 *
 * <p>Each service keeps its own thin {@code @Service} that implements its
 * domain port and delegates to this class, so the hexagonal port/adapter
 * boundary is preserved in the consuming modules.
 *
 * <p>A single {@code Gson} instance is shared (static final) because
 * {@code Gson} is thread-safe.
 *
 * <p>Callers must declare {@code spring-kafka} as an {@code implementation}
 * dependency; sky-common keeps it {@code compileOnly}.
 */
public final class KafkaNotificationPublisher {

    private static final Gson GSON = new Gson();

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public KafkaNotificationPublisher(KafkaTemplate<String, String> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    /**
     * Converts {@code message} to JSON and sends it to the configured topic.
     *
     * @param message the payload to publish; must not be {@code null}
     */
    public void publish(KafkaPayloadModel message) {
        kafkaTemplate.send(topic, GSON.toJson(message));
    }
}
