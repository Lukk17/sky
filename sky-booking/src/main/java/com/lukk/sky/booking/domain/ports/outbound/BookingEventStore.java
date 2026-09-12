package com.lukk.sky.booking.domain.ports.outbound;

import com.lukk.sky.booking.domain.model.Event;

import java.util.Optional;
import java.util.UUID;

public interface BookingEventStore {

    Optional<Integer> findLastSequenceNumber(UUID bookingId);

    /**
     * Appends the event and makes the insert visible before returning.
     *
     * @throws com.lukk.sky.booking.domain.exception.EventSequenceConflictException when the sequence number is taken
     */
    Event append(Event event);
}
