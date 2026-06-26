package com.lukk.sky.booking.domain.ports.service;

import com.google.gson.Gson;
import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.domain.model.Booking;
import com.lukk.sky.booking.domain.model.Event;
import com.lukk.sky.booking.domain.model.EventType;
import com.lukk.sky.booking.domain.ports.repository.EventSourceRepository;
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
    public void saveEvent(Booking booking, EventType eventType) {
        Assert.notNull(booking.getId(), "Booking id must not be null when saving an event");

        long lockKey = booking.getId().getMostSignificantBits() ^ booking.getId().getLeastSignificantBits();
        eventSourceRepository.lockBookingEventStream(lockKey);

        int lastSequence = eventSourceRepository.findLastSequenceNumberByBookingId(booking.getId())
                .orElse(0);

        Event event = Event.builder()
                .bookingId(booking.getId())
                .sequenceNumber(lastSequence + 1)
                .eventType(eventType)
                .payload(GSON.toJson(BookingDTO.of(booking)))
                .timestamp(Instant.now())
                .build();

        eventSourceRepository.save(event);
    }
}
