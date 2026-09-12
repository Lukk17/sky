package com.lukk.sky.offer.adapters.outbound.persistence;

import com.lukk.sky.offer.domain.exception.EventSequenceConflictException;
import com.lukk.sky.offer.domain.model.Event;
import com.lukk.sky.offer.domain.ports.outbound.EventSourceRepository;
import com.lukk.sky.offer.domain.ports.outbound.OfferEventStore;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

import static com.lukk.sky.offer.adapters.outbound.persistence.EventSpecifications.SEQUENCE_NUMBER;
import static com.lukk.sky.offer.adapters.outbound.persistence.EventSpecifications.hasOfferId;

@Component
@RequiredArgsConstructor
public class JpaOfferEventStore implements OfferEventStore {

    private static final Sort LATEST_FIRST = Sort.by(Sort.Direction.DESC, SEQUENCE_NUMBER);

    private final EventSourceRepository eventSourceRepository;

    @Override
    public Optional<Integer> findLastSequenceNumber(UUID offerId) {
        Optional<Event> latest = eventSourceRepository.findBy(
                hasOfferId(offerId),
                query -> query.sortBy(LATEST_FIRST).first());

        return latest.map(Event::getSequenceNumber);
    }

    @Override
    public Event append(Event event) {
        try {
            return eventSourceRepository.saveAndFlush(event);

        } catch (DataIntegrityViolationException alreadyTaken) {
            throw new EventSequenceConflictException(
                    "Sequence number %d is already taken for offer %s"
                            .formatted(event.getSequenceNumber(), event.getOfferId()),
                    alreadyTaken);
        }
    }
}
