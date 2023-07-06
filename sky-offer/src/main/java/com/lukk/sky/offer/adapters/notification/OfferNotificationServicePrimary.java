package com.lukk.sky.offer.adapters.notification;

import com.google.gson.Gson;
import com.lukk.sky.offer.adapters.dto.KafkaPayloadModel;
import com.lukk.sky.offer.domain.ports.notification.OfferNotificationService;
import lombok.Data;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.lukk.sky.offer.config.Constants.KAFKA_TOPIC;

@Service
@Data
public class OfferNotificationServicePrimary implements OfferNotificationService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public void sendMessage(KafkaPayloadModel message) {
        Gson gson = new Gson();

        kafkaTemplate.send(KAFKA_TOPIC, gson.toJson(message));
    }
}
