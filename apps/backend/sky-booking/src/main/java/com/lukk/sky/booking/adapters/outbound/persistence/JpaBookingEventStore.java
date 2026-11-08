package com.lukk.sky.booking.adapters.outbound.persistence;

import com.lukk.sky.booking.domain.exception.EventSequenceConflictException;
import com.lukk.sky.booking.domain.model.Event;
import com.lukk.sky.booking.domain.ports.outbound.BookingEventStore;
import com.lukk.sky.booking.domain.ports.outbound.EventSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

import static com.lukk.sky.booking.adapters.outbound.persistence.EventSpecifications.SEQUENCE_NUMBER;
import static com.lukk.sky.booking.adapters.outbound.persistence.EventSpecifications.hasBookingId;

@Component
@RequiredArgsConstructor
public class JpaBookingEventStore implements BookingEventStore {

    private static final Sort LATEST_FIRST = Sort.by(Sort.Direction.DESC, SEQUENCE_NUMBER);

    private final EventSourceRepository eventSourceRepository;

    @Override
    public Optional<Integer> findLastSequenceNumber(UUID bookingId) {
        Optional<Event> latest = eventSourceRepository.findBy(
                hasBookingId(bookingId),
                query -> query.sortBy(LATEST_FIRST).first());

        return latest.map(Event::getSequenceNumber);
    }

    @Override
    public Event append(Event event) {
        try {
            return eventSourceRepository.saveAndFlush(event);

        } catch (DataIntegrityViolationException alreadyTaken) {
            throw new EventSequenceConflictException(
                    "Sequence number %d is already taken for booking %s"
                            .formatted(event.getSequenceNumber(), event.getBookingId()),
                    alreadyTaken);
        }
    }
}
