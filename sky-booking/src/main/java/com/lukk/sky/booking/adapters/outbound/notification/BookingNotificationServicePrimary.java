package com.lukk.sky.booking.adapters.outbound.notification;

import com.lukk.sky.booking.domain.ports.outbound.BookingNotificationService;
import com.lukk.sky.common.kafka.KafkaNotificationPublisher;
import com.lukk.sky.common.kafka.KafkaPayloadModel;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import static com.lukk.sky.common.kafka.SkyTopics.BOOKING_TOPIC;

@Service
@Primary
public class BookingNotificationServicePrimary implements BookingNotificationService {

    private final KafkaNotificationPublisher publisher;

    public BookingNotificationServicePrimary(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.publisher = new KafkaNotificationPublisher(kafkaTemplate, objectMapper, BOOKING_TOPIC);
    }

    @Override
    public void sendMessage(KafkaPayloadModel message) {
        publisher.publish(message);
    }
}
