package com.lukk.sky.booking.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Event identity contract")
class EventTest {

    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000cc");
    private static final UUID BOOKING_ID = UUID.fromString("00000000-0000-0000-0000-0000000000dd");
    private static final Instant TIMESTAMP = Instant.parse("2201-06-20T14:35:47Z");

    @Test
    @DisplayName("equals is false for two unsaved events even when every business field matches")
    void equals_whenBothEventsAreUnsaved_thenNotEqual() {
        Event one = event(null, 1);
        Event other = event(null, 1);

        assertThat(one).isNotEqualTo(other);
    }

    @Test
    @DisplayName("equals is true for the same identifier even when the sequence number differs")
    void equals_whenSameIdentifierAndDifferentSequenceNumber_thenEqual() {
        Event one = event(EVENT_ID, 1);
        Event other = event(EVENT_ID, 7);

        assertThat(one).isEqualTo(other);
    }

    @Test
    @DisplayName("equals is false for different identifiers")
    void equals_whenDifferentIdentifiers_thenNotEqual() {
        Event one = event(EVENT_ID, 1);
        Event other = event(UUID.randomUUID(), 1);

        assertThat(one).isNotEqualTo(other);
    }

    @Test
    @DisplayName("hashCode does not change when the identifier is assigned on persist")
    void hashCode_whenIdentifierIsAssignedAfterInsertionIntoASet_thenTheEntityIsStillFound() {
        Event event = event(null, 1);
        Set<Event> events = new HashSet<>();
        events.add(event);
        int hashBeforePersist = event.hashCode();

        event.setId(EVENT_ID);

        assertThat(event.hashCode()).isEqualTo(hashBeforePersist);
        assertThat(events).contains(event);
    }

    @Test
    @DisplayName("equals is false against null and against a value of another type")
    void equals_whenComparedWithNullOrAValueOfAnotherType_thenNotEqual() {
        Event event = event(EVENT_ID, 1);

        assertThat(event.equals(null)).isFalse();
        assertThat(event.equals(EVENT_ID.toString())).isFalse();
    }

    private static Event event(UUID id, int sequenceNumber) {
        return Event.builder()
                .id(id)
                .bookingId(BOOKING_ID)
                .sequenceNumber(sequenceNumber)
                .eventType(EventType.BOOKED)
                .payload("{}")
                .timestamp(TIMESTAMP)
                .build();
    }
}
