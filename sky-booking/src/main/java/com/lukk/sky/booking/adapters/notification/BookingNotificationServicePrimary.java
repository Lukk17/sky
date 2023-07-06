package com.lukk.sky.booking.adapters.notification;

import com.google.gson.Gson;
import com.lukk.sky.booking.adapters.dto.KafkaPayloadModel;
import com.lukk.sky.booking.domain.ports.notification.BookingNotificationService;
import lombok.Data;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.lukk.sky.booking.config.Constants.KAFKA_TOPIC;


@Service
@Data
public class BookingNotificationServicePrimary implements BookingNotificationService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public void sendMessage(KafkaPayloadModel message) {
        Gson gson = new Gson();

        kafkaTemplate.send(KAFKA_TOPIC, gson.toJson(message));
    }
}
