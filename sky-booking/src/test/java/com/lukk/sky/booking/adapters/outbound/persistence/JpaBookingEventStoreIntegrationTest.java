package com.lukk.sky.booking.adapters.outbound.persistence;

import com.lukk.sky.booking.AbstractIntegrationTest;
import com.lukk.sky.booking.domain.exception.EventSequenceConflictException;
import com.lukk.sky.booking.domain.model.Event;
import com.lukk.sky.booking.domain.model.EventType;
import com.lukk.sky.booking.domain.ports.outbound.BookingEventStore;
import com.lukk.sky.booking.domain.ports.outbound.EventSourceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JpaBookingEventStore integration tests")
class JpaBookingEventStoreIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private BookingEventStore bookingEventStore;

    @Autowired
    private EventSourceRepository eventSourceRepository;

    @AfterEach
    void tearDown() {
        eventSourceRepository.deleteAll();
    }

    @Test
    @DisplayName("findLastSequenceNumber returns empty when the booking has no events yet")
    void findLastSequenceNumber_whenNoEventExists_thenReturnEmpty() {
        assertThat(bookingEventStore.findLastSequenceNumber(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("findLastSequenceNumber returns the highest sequence number regardless of insert order")
    void findLastSequenceNumber_whenSeveralEventsExist_thenReturnTheHighestSequenceNumber() {
        // given
        UUID bookingId = UUID.randomUUID();
        appendEvent(bookingId, 1);
        appendEvent(bookingId, 3);
        appendEvent(bookingId, 2);

        // when / then
        assertThat(bookingEventStore.findLastSequenceNumber(bookingId)).contains(3);
    }

    @Test
    @DisplayName("findLastSequenceNumber ignores events that belong to another booking")
    void findLastSequenceNumber_whenAnotherBookingHasAHigherSequence_thenIgnoreIt() {
        // given
        UUID bookingId = UUID.randomUUID();
        appendEvent(bookingId, 2);
        appendEvent(UUID.randomUUID(), 9);

        // when / then
        assertThat(bookingEventStore.findLastSequenceNumber(bookingId)).contains(2);
    }

    @Test
    @DisplayName("append persists the event and makes it visible to the sequence lookup")
    void append_whenCalled_thenTheEventIsPersisted() {
        // given
        UUID bookingId = UUID.randomUUID();

        // when
        Event appended = bookingEventStore.append(event(bookingId, 1));

        // then
        assertThat(appended.getId()).isNotNull();
        assertThat(eventSourceRepository.findById(appended.getId())).isPresent();
        assertThat(bookingEventStore.findLastSequenceNumber(bookingId)).contains(1);
    }

    @Test
    @DisplayName("append reports a conflict when the booking already has an event with that sequence number")
    void append_whenTheSequenceNumberIsAlreadyTaken_thenThrowEventSequenceConflictException() {
        // given
        UUID bookingId = UUID.randomUUID();
        bookingEventStore.append(event(bookingId, 1));

        // when / then
        assertThatThrownBy(() -> bookingEventStore.append(event(bookingId, 1)))
                .isInstanceOf(EventSequenceConflictException.class)
                .hasMessageContaining(bookingId.toString());
    }

    private void appendEvent(UUID bookingId, int sequenceNumber) {
        eventSourceRepository.save(event(bookingId, sequenceNumber));
    }

    private static Event event(UUID bookingId, int sequenceNumber) {
        return Event.builder()
                .bookingId(bookingId)
                .sequenceNumber(sequenceNumber)
                .eventType(EventType.BOOKED)
                .payload("{}")
                .timestamp(Instant.now())
                .build();
    }
}
