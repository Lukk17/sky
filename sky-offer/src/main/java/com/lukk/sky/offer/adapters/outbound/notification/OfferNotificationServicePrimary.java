package com.lukk.sky.offer.adapters.outbound.notification;

import com.lukk.sky.common.kafka.KafkaNotificationPublisher;
import com.lukk.sky.common.kafka.KafkaPayloadModel;
import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.domain.ports.outbound.OfferNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static com.lukk.sky.common.web.DateTimeConstants.DATE_TIME_FORMAT;
import static com.lukk.sky.offer.config.Constants.KAFKA_TOPIC;

/**
 * Primary implementation of the {@link OfferNotificationService}.
 * Builds the wire envelope and delegates serialisation and Kafka dispatch to {@link KafkaNotificationPublisher}.
 */
@Service
@Primary
@Slf4j
public class OfferNotificationServicePrimary implements OfferNotificationService {

    private static final String DELETED_MESSAGE_FORMAT = "Offer with ID: %s was deleted.";

    private final KafkaNotificationPublisher publisher;
    private final ObjectMapper objectMapper;

    public OfferNotificationServicePrimary(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.publisher = new KafkaNotificationPublisher(kafkaTemplate, objectMapper, KAFKA_TOPIC);
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishCreated(OfferDTO offer, String ownerEmail) {
        publish(objectMapper.writeValueAsString(offer), ownerEmail);
    }

    @Override
    public void publishEdited(OfferDTO offer, String ownerEmail) {
        publish(objectMapper.writeValueAsString(offer), ownerEmail);
    }

    @Override
    public void publishDeleted(UUID offerId, String ownerEmail) {
        publish(String.format(DELETED_MESSAGE_FORMAT, offerId), ownerEmail);
    }

    private void publish(String payload, String ownerEmail) {
        log.info("Publishing to Kafka");

        publisher.publish(new KafkaPayloadModel(payload, DATE_TIME_FORMAT.format(Instant.now()), ownerEmail));
    }
}
