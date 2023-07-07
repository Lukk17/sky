package com.lukk.sky.booking.domain.ports.service;

import com.google.gson.Gson;
import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.domain.model.Booking;
import com.lukk.sky.booking.domain.model.Event;
import com.lukk.sky.booking.domain.model.EventType;
import com.lukk.sky.booking.domain.ports.repository.EventSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EventSourceServicePrimary implements EventSourceService {

    private final EventSourceRepository eventSourceRepository;

    @Override
    public void saveEvent(Booking booking, EventType eventType) {
        Gson gson = new Gson();

        int lastSequence = eventSourceRepository.findLastSequenceNumberByOfferId(String.valueOf(booking.getId()))
                .orElse(0);

        Event event = Event.builder()
                .bookingId(booking.getId())
                .sequenceNumber(lastSequence + 1)
                .eventType(eventType)
                .payload(gson.toJson(BookingDTO.of(booking)))
                .timestamp(LocalDateTime.now())
                .build();

        eventSourceRepository.save(event);
    }
}
