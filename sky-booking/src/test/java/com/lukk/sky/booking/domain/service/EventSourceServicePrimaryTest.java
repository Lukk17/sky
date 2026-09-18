package com.lukk.sky.booking.domain.service;

import com.lukk.sky.booking.assemblers.BookingAssembler;
import com.lukk.sky.booking.domain.exception.EventSequenceConflictException;
import com.lukk.sky.booking.domain.model.Booking;
import com.lukk.sky.booking.domain.model.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static com.lukk.sky.booking.assemblers.BookingAssembler.TEST_DEFAULT_BOOKED_ID;
import static com.lukk.sky.booking.assemblers.EventAssembler.TEST_EVENT_TYPE;
import static com.lukk.sky.booking.domain.service.EventSourceServicePrimary.MAX_APPEND_ATTEMPTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("EventSourceServicePrimary unit tests")
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class EventSourceServicePrimaryTest {

    @Mock
    BookingEventAppender bookingEventAppender;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private EventSourceServicePrimary eventSourceServicePrimary;

    @BeforeEach
    void setUp() {
        eventSourceServicePrimary = new EventSourceServicePrimary(bookingEventAppender, objectMapper);
    }

    @Test
    @DisplayName("saveEvent appends one event carrying the booking id and the event type")
    void saveEvent_whenCalled_thenAppendOneEventForThatBookingAndEventType() {
        // given
        Booking booking = BookingAssembler.getPopulatedBooked();

        // when
        eventSourceServicePrimary.saveEvent(booking, TEST_EVENT_TYPE);

        // then
        verify(bookingEventAppender, times(1))
                .appendNextEvent(eq(TEST_DEFAULT_BOOKED_ID), eq(TEST_EVENT_TYPE), anyString());
    }

    @Test
    @DisplayName("saveEvent serialises the booking payload as JSON that carries every booking field")
    void saveEvent_whenSaving_thenPayloadIsJsonWithEveryBookingField() {
        // given
        Booking booking = BookingAssembler.getPopulatedBooked();

        // when
        eventSourceServicePrimary.saveEvent(booking, TEST_EVENT_TYPE);

        // then
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(bookingEventAppender)
                .appendNextEvent(eq(TEST_DEFAULT_BOOKED_ID), eq(TEST_EVENT_TYPE), payload.capture());
        JsonNode serialised = objectMapper.readTree(payload.getValue());
        assertEquals(booking.getId().toString(), serialised.get("id").asString());
        assertEquals(booking.getOfferId().toString(), serialised.get("offerId").asString());
        assertEquals(booking.getBookedDate().toString(), serialised.get("bookedDate").asString());
        assertEquals(booking.getBookingUser(), serialised.get("bookingUser").asString());
        assertEquals(booking.getOwnerEmail(), serialised.get("ownerEmail").asString());
    }

    @Test
    @DisplayName("saveEvent retries the append when another writer took the sequence number first")
    void saveEvent_whenFirstAttemptsConflict_thenRetryUntilTheAppendSucceeds() {
        // given
        Booking booking = BookingAssembler.getPopulatedBooked();
        when(bookingEventAppender.appendNextEvent(eq(TEST_DEFAULT_BOOKED_ID), eq(TEST_EVENT_TYPE), anyString()))
                .thenThrow(conflict())
                .thenThrow(conflict())
                .thenReturn(null);

        // when
        eventSourceServicePrimary.saveEvent(booking, TEST_EVENT_TYPE);

        // then
        verify(bookingEventAppender, times(3))
                .appendNextEvent(eq(TEST_DEFAULT_BOOKED_ID), eq(TEST_EVENT_TYPE), anyString());
    }

    @Test
    @DisplayName("saveEvent gives up after the bounded number of attempts, with the last conflict as the cause")
    void saveEvent_whenEveryAttemptConflicts_thenThrowAfterMaxAttemptsWithTheLastConflictAsCause() {
        // given
        Booking booking = BookingAssembler.getPopulatedBooked();
        EventSequenceConflictException everyAttemptConflicts = conflict();
        when(bookingEventAppender.appendNextEvent(eq(TEST_DEFAULT_BOOKED_ID), eq(TEST_EVENT_TYPE), anyString()))
                .thenThrow(everyAttemptConflicts);

        // when
        EventSequenceConflictException thrown = assertThrows(EventSequenceConflictException.class,
                () -> eventSourceServicePrimary.saveEvent(booking, TEST_EVENT_TYPE));

        // then
        verify(bookingEventAppender, times(MAX_APPEND_ATTEMPTS))
                .appendNextEvent(eq(TEST_DEFAULT_BOOKED_ID), eq(TEST_EVENT_TYPE), anyString());
        assertSame(everyAttemptConflicts, thrown.getCause());
        assertThat(thrown).hasMessageContaining(String.valueOf(MAX_APPEND_ATTEMPTS));
    }

    @Test
    @DisplayName("saveEvent throws IllegalArgumentException and persists nothing when the booking id is null")
    void saveEvent_whenBookingIdIsNull_thenThrowIllegalArgumentException() {
        // given
        Booking booking = Booking.builder()
                .offerId(UUID.randomUUID())
                .bookingUser("user@test.com")
                .ownerEmail("owner@test.com")
                .build();

        // when / then
        assertThrows(IllegalArgumentException.class,
                () -> eventSourceServicePrimary.saveEvent(booking, TEST_EVENT_TYPE));

        verify(bookingEventAppender, never()).appendNextEvent(any(UUID.class), any(EventType.class), anyString());
    }

    private static EventSequenceConflictException conflict() {
        return new EventSequenceConflictException("sequence number already taken",
                new IllegalStateException("duplicate key"));
    }
}
