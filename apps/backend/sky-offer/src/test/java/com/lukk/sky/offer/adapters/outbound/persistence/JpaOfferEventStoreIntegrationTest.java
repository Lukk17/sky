package com.lukk.sky.offer.adapters.outbound.persistence;

import com.lukk.sky.offer.AbstractIntegrationTest;
import com.lukk.sky.offer.domain.exception.EventSequenceConflictException;
import com.lukk.sky.offer.domain.model.Event;
import com.lukk.sky.offer.domain.model.EventType;
import com.lukk.sky.offer.domain.ports.outbound.EventSourceRepository;
import com.lukk.sky.offer.domain.ports.outbound.OfferEventStore;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JpaOfferEventStore: the Specification lookup returns the same row as the JPQL it replaced")
class JpaOfferEventStoreIntegrationTest extends AbstractIntegrationTest {

    private static final String LEGACY_MAX_SEQUENCE_JPQL =
            "SELECT max(e.sequenceNumber) FROM Event e WHERE e.offerId = ?1";

    @Autowired
    private OfferEventStore offerEventStore;

    @Autowired
    private EventSourceRepository eventSourceRepository;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void clearEvents() {
        eventSourceRepository.deleteAll();
    }

    @Test
    @DisplayName("findLastSequenceNumber returns empty when the offer has no events yet, as the JPQL query did")
    void findLastSequenceNumber_whenNoEventExists_thenReturnEmpty() {
        // given
        UUID offerId = UUID.randomUUID();

        // when / then
        assertThat(offerEventStore.findLastSequenceNumber(offerId)).isEmpty();
        assertThat(legacyMaxSequenceNumber(offerId)).isEmpty();
    }

    @Test
    @DisplayName("findLastSequenceNumber returns the highest sequence number regardless of insert order")
    void findLastSequenceNumber_whenSeveralEventsExist_thenReturnTheHighestSequenceNumber() {
        // given
        UUID offerId = UUID.randomUUID();
        saveEvent(offerId, 1);
        saveEvent(offerId, 3);
        saveEvent(offerId, 2);

        // when / then
        assertThat(offerEventStore.findLastSequenceNumber(offerId)).contains(3);
        assertThat(offerEventStore.findLastSequenceNumber(offerId)).isEqualTo(legacyMaxSequenceNumber(offerId));
    }

    @Test
    @DisplayName("findLastSequenceNumber ignores events that belong to another offer")
    void findLastSequenceNumber_whenAnotherOfferHasAHigherSequence_thenIgnoreIt() {
        // given
        UUID offerId = UUID.randomUUID();
        saveEvent(offerId, 2);
        saveEvent(UUID.randomUUID(), 9);

        // when / then
        assertThat(offerEventStore.findLastSequenceNumber(offerId)).contains(2);
        assertThat(offerEventStore.findLastSequenceNumber(offerId)).isEqualTo(legacyMaxSequenceNumber(offerId));
    }

    @Test
    @DisplayName("append persists the event and makes it visible to the sequence lookup")
    void append_whenCalled_thenTheEventIsPersisted() {
        // given
        UUID offerId = UUID.randomUUID();

        // when
        Event appended = offerEventStore.append(event(offerId, 1));

        // then
        assertThat(appended.getId()).isNotNull();
        assertThat(eventSourceRepository.findById(appended.getId())).isPresent();
        assertThat(offerEventStore.findLastSequenceNumber(offerId)).contains(1);
    }

    @Test
    @DisplayName("append reports a conflict when the offer already has an event with that sequence number")
    void append_whenTheSequenceNumberIsAlreadyTaken_thenThrowEventSequenceConflictException() {
        // given
        UUID offerId = UUID.randomUUID();
        offerEventStore.append(event(offerId, 1));

        // when / then
        assertThatThrownBy(() -> offerEventStore.append(event(offerId, 1)))
                .isInstanceOf(EventSequenceConflictException.class)
                .hasMessageContaining(offerId.toString());
    }

    private Optional<Integer> legacyMaxSequenceNumber(UUID offerId) {
        return Optional.ofNullable(entityManager.createQuery(LEGACY_MAX_SEQUENCE_JPQL, Integer.class)
                .setParameter(1, offerId)
                .getSingleResult());
    }

    private void saveEvent(UUID offerId, int sequenceNumber) {
        eventSourceRepository.save(event(offerId, sequenceNumber));
    }

    private static Event event(UUID offerId, int sequenceNumber) {
        return Event.builder()
                .offerId(offerId)
                .sequenceNumber(sequenceNumber)
                .eventType(EventType.OFFER_CREATED)
                .payload("{}")
                .timestamp(Instant.now())
                .build();
    }
}
