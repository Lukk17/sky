package com.lukk.sky.offer.domain.ports.service;

import com.lukk.sky.offer.domain.model.EventType;
import com.lukk.sky.offer.domain.model.Offer;

/**
 * Service for managing events in the system. An event is generated
 * whenever an offer is created, updated or deleted.
 */
public interface EventSourceService {
    /**
     * Saves the given event information, determined by the provided offer and event type.
     *
     * @param savedOffer The offer to associate with the event. Must not be {@code null}.
     * @param eventType  The type of event that is being saved. Must not be {@code null}.
     * @throws IllegalArgumentException if either {@code savedOffer} or {@code eventType} is {@code null}.
     */
    void saveEvent(Offer savedOffer, EventType eventType);
}
