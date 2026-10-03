package com.lukk.sky.offer.domain.service;

import com.lukk.sky.common.domain.SequenceEventAppender;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Event appendNextEvent(UUID offerId, EventType eventType, String payload) {
        int next = SequenceEventAppender.nextSequenceNumber(offerId, offerEventStore::findLastSequenceNumber);
        Event event = Event.builder()
                .offerId(offerId)
                .sequenceNumber(next)
                .eventType(eventType)
                .payload(payload)
                .timestamp(Instant.now())
                .build();
        return offerEventStore.append(event);
    }
}
