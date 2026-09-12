package com.lukk.sky.offer.adapters.outbound.notification;

import com.lukk.sky.common.kafka.KafkaNotificationPublisher;
import com.lukk.sky.common.kafka.KafkaPayloadModel;
import com.lukk.sky.offer.domain.ports.outbound.OfferNotificationService;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import static com.lukk.sky.offer.config.Constants.KAFKA_TOPIC;

/**
 * Primary implementation of the {@link OfferNotificationService}.
 * Delegates serialisation and Kafka dispatch to {@link KafkaNotificationPublisher}.
 */
@Service
@Primary
public class OfferNotificationServicePrimary implements OfferNotificationService {

    private final KafkaNotificationPublisher publisher;

    public OfferNotificationServicePrimary(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.publisher = new KafkaNotificationPublisher(kafkaTemplate, objectMapper, KAFKA_TOPIC);
    }

    @Override
    public void sendMessage(KafkaPayloadModel message) {
        publisher.publish(message);
    }
}
