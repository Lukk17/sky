package com.lukk.sky.booking.domain.ports.service;

import com.google.gson.Gson;
import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.domain.model.Booking;
import com.lukk.sky.booking.domain.model.Event;
import com.lukk.sky.booking.domain.model.EventType;
import com.lukk.sky.booking.domain.ports.repository.EventSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Primary implementation of the {@link EventSourceService}.
 * It uses {@link EventSourceRepository} to perform operations on the database.
 */
@Service
@RequiredArgsConstructor
@Primary
public class EventSourceServicePrimary implements EventSourceService {

    private final EventSourceRepository eventSourceRepository;

    /**
     * {@inheritDoc}
     * <p>This implementation does more than just save an event:
     * <ul>
     *     <li>It uses Gson to convert the booking to JSON format for the payload of the event.</li>
     *     <li>It retrieves the last sequence number associated with the booking's ID and increments it for the event's sequence number.</li>
     *     <li>It sets the current time for the event's timestamp.</li>
     * </ul>
     * The resulting event is then saved to the repository.
     */
    @Override
    public void saveEvent(Booking booking, EventType eventType) {
        Gson gson = new Gson();

        int lastSequence = eventSourceRepository.findLastSequenceNumberByBookingId(String.valueOf(booking.getId()))
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
