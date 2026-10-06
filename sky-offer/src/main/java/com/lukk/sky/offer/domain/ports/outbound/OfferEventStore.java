package com.lukk.sky.offer.domain.ports.outbound;

import com.lukk.sky.offer.domain.model.Event;

import java.util.Optional;
import java.util.UUID;

public interface OfferEventStore {

    Optional<Integer> findLastSequenceNumber(UUID offerId);

    /**
     * Appends the event and makes the insert visible before returning.
     *
     * @throws com.lukk.sky.offer.domain.exception.EventSequenceConflictException when the sequence number is taken
     */
    Event append(Event event);
}
