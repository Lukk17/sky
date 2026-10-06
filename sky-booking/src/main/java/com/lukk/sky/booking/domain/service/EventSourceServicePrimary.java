package com.lukk.sky.booking.domain.service;

import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.domain.exception.EventSequenceConflictException;
import com.lukk.sky.booking.domain.model.Booking;
import com.lukk.sky.booking.domain.model.EventType;
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

    private final BookingEventAppender bookingEventAppender;
    private final ObjectMapper objectMapper;

    @Override
    public void saveEvent(Booking booking, EventType eventType) {
        Assert.notNull(booking.getId(), "Booking id must not be null when saving an event");

        String payload = objectMapper.writeValueAsString(BookingDTO.of(booking));

        appendRetryingSequenceConflicts(booking.getId(), eventType, payload);
    }

    private void appendRetryingSequenceConflicts(UUID bookingId, EventType eventType, String payload) {
        EventSequenceConflictException lastConflict = null;

        for (int attempt = 1; attempt <= MAX_APPEND_ATTEMPTS; attempt++) {
            try {
                bookingEventAppender.appendNextEvent(bookingId, eventType, payload);

                return;

            } catch (EventSequenceConflictException conflict) {
                lastConflict = conflict;

                log.warn("event_append_conflict bookingId={} eventType={} attempt={} maxAttempts={}",
                        bookingId, eventType, attempt, MAX_APPEND_ATTEMPTS);
            }
        }

        throw new EventSequenceConflictException(
                "Gave up appending a %s event for booking %s after %d attempts"
                        .formatted(eventType, bookingId, MAX_APPEND_ATTEMPTS),
                lastConflict);
    }
}
