package com.lukk.sky.booking.domain.service;

import com.lukk.sky.booking.domain.exception.EventSequenceConflictException;
import com.lukk.sky.booking.domain.model.Event;
import com.lukk.sky.booking.domain.model.EventType;
import com.lukk.sky.booking.domain.ports.outbound.BookingEventStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BookingEventAppender {

    private final BookingEventStore bookingEventStore;

    /**
     * Appends the event one past the highest committed sequence number, in a transaction of its own so a
     * caller can retry a losing attempt without the surrounding transaction being marked rollback-only.
     *
     * @throws EventSequenceConflictException when another writer committed that sequence number first
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Event appendNextEvent(UUID bookingId, EventType eventType, String payload) {
        int nextSequenceNumber = bookingEventStore.findLastSequenceNumber(bookingId).orElse(0) + 1;

        Event event = Event.builder()
                .bookingId(bookingId)
                .sequenceNumber(nextSequenceNumber)
                .eventType(eventType)
                .payload(payload)
                .timestamp(Instant.now())
                .build();

        return bookingEventStore.append(event);
    }
}
