package com.lukk.sky.offer.domain.ports.service;

import com.google.gson.Gson;
import com.lukk.sky.offer.domain.model.Event;
import com.lukk.sky.offer.domain.model.EventType;
import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.ports.repository.EventSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
@Primary
public class EventSourceServicePrimary implements EventSourceService {

    private static final Gson GSON = new Gson();

    private final EventSourceRepository eventSourceRepository;

    @Override
    @Transactional
    public void saveEvent(Offer offer, EventType eventType) {
        Assert.notNull(offer.getId(), "Offer id must not be null when saving an event");

        eventSourceRepository.lockOfferEventStream(offer.getId());

        int lastSequence = eventSourceRepository.findLastSequenceNumberByOfferId(offer.getId())
                .orElse(0);

        Event event = Event.builder()
                .offerId(offer.getId())
                .sequenceNumber(lastSequence + 1)
                .eventType(eventType)
                .payload(GSON.toJson(offer))
                .timestamp(Instant.now())
                .build();

        eventSourceRepository.save(event);
    }
}
