package com.lukk.sky.booking.domain.service;

import com.lukk.sky.booking.domain.model.Event;
import com.lukk.sky.booking.domain.model.EventType;
import com.lukk.sky.booking.domain.ports.outbound.BookingEventStore;
import com.lukk.sky.common.domain.SequenceEventAppender;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Event appendNextEvent(UUID bookingId, EventType eventType, String payload) {
        int next = SequenceEventAppender.nextSequenceNumber(bookingId, bookingEventStore::findLastSequenceNumber);
        Event event = Event.builder()
                .bookingId(bookingId)
                .sequenceNumber(next)
                .eventType(eventType)
                .payload(payload)
                .timestamp(Instant.now())
                .build();
        return bookingEventStore.append(event);
    }
}
