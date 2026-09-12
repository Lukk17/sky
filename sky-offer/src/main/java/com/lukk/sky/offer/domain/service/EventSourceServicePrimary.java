package com.lukk.sky.offer.domain.service;

import com.lukk.sky.offer.domain.exception.EventSequenceConflictException;
import com.lukk.sky.offer.domain.model.EventType;
import com.lukk.sky.offer.domain.model.Offer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Primary
public class EventSourceServicePrimary implements EventSourceService {

    static final int MAX_APPEND_ATTEMPTS = 20;

    private final OfferEventAppender offerEventAppender;
    private final ObjectMapper objectMapper;

    @Override
    public void saveEvent(Offer offer, EventType eventType) {
        Assert.notNull(offer.getId(), "Offer id must not be null when saving an event");

        String payload = objectMapper.writeValueAsString(offer);

        appendRetryingSequenceConflicts(offer.getId(), eventType, payload);
    }

    private void appendRetryingSequenceConflicts(UUID offerId, EventType eventType, String payload) {
        EventSequenceConflictException lastConflict = null;

        for (int attempt = 1; attempt <= MAX_APPEND_ATTEMPTS; attempt++) {
            try {
                offerEventAppender.appendNextEvent(offerId, eventType, payload);

                return;

            } catch (EventSequenceConflictException conflict) {
                lastConflict = conflict;

                log.warn("event_append_conflict offerId={} eventType={} attempt={} maxAttempts={}",
                        offerId, eventType, attempt, MAX_APPEND_ATTEMPTS);
            }
        }

        throw new EventSequenceConflictException(
                "Gave up appending a %s event for offer %s after %d attempts"
                        .formatted(eventType, offerId, MAX_APPEND_ATTEMPTS),
                lastConflict);
    }
}
