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

import java.time.LocalDateTime;

/**
 * The primary implementation of the {@link EventSourceService} interface.
 * This implementation uses an {@link EventSourceRepository} to persist events.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Primary
public class EventSourceServicePrimary implements EventSourceService {

    private final EventSourceRepository eventSourceRepository;

    /**
     * {@inheritDoc}
     * <p>
     * The event is saved with the next sequence number for the given offer,
     * along with the current timestamp. The offer is serialized to JSON format
     * and saved as the event payload.
     */
    @Override
    public void saveEvent(Offer offer, EventType eventType) {
        Gson gson = new Gson();

        int lastSequence = eventSourceRepository.findLastSequenceNumberByOfferId(String.valueOf(offer.getId()))
                .orElse(0);

        Event event = Event.builder()
                .offerId(offer.getId())
                .sequenceNumber(lastSequence + 1)
                .eventType(eventType)
                .payload(gson.toJson(offer))
                .timestamp(LocalDateTime.now())
                .build();

        eventSourceRepository.save(event);
    }
}
