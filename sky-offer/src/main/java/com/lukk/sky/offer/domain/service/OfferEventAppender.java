package com.lukk.sky.offer.domain.service;

import com.lukk.sky.offer.domain.exception.EventSequenceConflictException;
import com.lukk.sky.offer.domain.model.Event;
import com.lukk.sky.offer.domain.model.EventType;
import com.lukk.sky.offer.domain.ports.outbound.OfferEventStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OfferEventAppender {

    private final OfferEventStore offerEventStore;

    /**
     * Appends the event one past the highest committed sequence number, in a transaction of its own so a
     * caller can retry a losing attempt without the surrounding transaction being marked rollback-only.
     *
     * @throws EventSequenceConflictException when another writer committed that sequence number first
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Event appendNextEvent(UUID offerId, EventType eventType, String payload) {
        int nextSequenceNumber = offerEventStore.findLastSequenceNumber(offerId).orElse(0) + 1;

        Event event = Event.builder()
                .offerId(offerId)
                .sequenceNumber(nextSequenceNumber)
                .eventType(eventType)
                .payload(payload)
                .timestamp(Instant.now())
                .build();

        return offerEventStore.append(event);
    }
}
