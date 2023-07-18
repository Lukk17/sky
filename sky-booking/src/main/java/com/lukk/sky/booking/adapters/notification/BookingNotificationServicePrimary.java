package com.lukk.sky.booking.adapters.notification;

import com.google.gson.Gson;
import com.lukk.sky.booking.adapters.dto.KafkaPayloadModel;
import com.lukk.sky.booking.domain.ports.notification.BookingNotificationService;
import lombok.Data;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.lukk.sky.booking.config.Constants.KAFKA_TOPIC;

/**
 * Primary implementation of the {@link BookingNotificationService}.
 * It uses {@link KafkaTemplate} to send messages to a Kafka topic.
 */
@Service
@Data
@Primary
public class BookingNotificationServicePrimary implements BookingNotificationService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * {@inheritDoc}
     * <p>This implementation does more than just send a message:
     * <ul>
     *     <li>It uses Gson to convert the message to JSON format.</li>
     *     <li>It sends the JSON-formatted message to a predefined Kafka topic.</li>
     * </ul>
     */
    @Override
    public void sendMessage(KafkaPayloadModel message) {
        Gson gson = new Gson();

        kafkaTemplate.send(KAFKA_TOPIC, gson.toJson(message));
    }
}
