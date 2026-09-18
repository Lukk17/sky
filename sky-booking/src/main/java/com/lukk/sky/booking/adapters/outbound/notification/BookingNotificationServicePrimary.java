package com.lukk.sky.booking.adapters.outbound.notification;

import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.domain.ports.outbound.BookingNotificationService;
import com.lukk.sky.common.kafka.KafkaNotificationPublisher;
import com.lukk.sky.common.kafka.KafkaPayloadModel;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static com.lukk.sky.common.kafka.SkyTopics.BOOKING_TOPIC;
import static com.lukk.sky.common.web.DateTimeConstants.DATE_TIME_FORMAT;

@Service
@Primary
public class BookingNotificationServicePrimary implements BookingNotificationService {

    private final KafkaNotificationPublisher publisher;
    private final ObjectMapper objectMapper;

    public BookingNotificationServicePrimary(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.publisher = new KafkaNotificationPublisher(kafkaTemplate, objectMapper, BOOKING_TOPIC);
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishCreated(BookingDTO booking, String userEmail) {
        publish(objectMapper.writeValueAsString(booking), userEmail);
    }

    @Override
    public void publishRemoved(String removalMessage, String userEmail) {
        publish(removalMessage, userEmail);
    }

    private void publish(String payload, String userEmail) {
        publisher.publish(new KafkaPayloadModel(payload, DATE_TIME_FORMAT.format(Instant.now()), userEmail));
    }
}
