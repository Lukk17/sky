package com.lukk.sky.offer.adapters.notification;

import com.google.gson.Gson;
import com.lukk.sky.offer.adapters.dto.KafkaPayloadModel;
import com.lukk.sky.offer.domain.ports.notification.OfferNotificationService;
import lombok.Data;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.lukk.sky.offer.config.Constants.KAFKA_TOPIC;

/**
 * The primary implementation of the {@link OfferNotificationService} interface.
 * This implementation uses a {@link KafkaTemplate} to send the notifications.
 */
@Service
@Data
@Primary
public class OfferNotificationServicePrimary implements OfferNotificationService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * {@inheritDoc}
     * <p>
     * This implementation transforms the given {@link KafkaPayloadModel} into a JSON string using {@link Gson},
     * and then sends it to a Kafka topic using a {@link KafkaTemplate}.
     */
    @Override
    public void sendMessage(KafkaPayloadModel message) {
        Gson gson = new Gson();

        kafkaTemplate.send(KAFKA_TOPIC, gson.toJson(message));
    }
}
