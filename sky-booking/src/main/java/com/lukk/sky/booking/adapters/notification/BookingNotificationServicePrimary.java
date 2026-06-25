package com.lukk.sky.booking.adapters.notification;

import com.lukk.sky.common.kafka.KafkaNotificationPublisher;
import com.lukk.sky.common.kafka.KafkaPayloadModel;
import com.lukk.sky.booking.domain.ports.notification.BookingNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.lukk.sky.booking.config.Constants.KAFKA_TOPIC;

@Service
@Primary
public class BookingNotificationServicePrimary implements BookingNotificationService {

    private final KafkaNotificationPublisher publisher;

    public BookingNotificationServicePrimary(KafkaTemplate<String, String> kafkaTemplate) {
        this.publisher = new KafkaNotificationPublisher(kafkaTemplate, KAFKA_TOPIC);
    }

    @Override
    public void sendMessage(KafkaPayloadModel message) {
        publisher.publish(message);
    }
}
