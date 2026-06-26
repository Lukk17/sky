package com.lukk.sky.booking.domain.ports.service;

import com.google.gson.Gson;
import com.lukk.sky.booking.Assemblers.BookingAssembler;
import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.domain.model.Booking;
import com.lukk.sky.booking.domain.model.Event;
import com.lukk.sky.booking.domain.ports.repository.EventSourceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.lukk.sky.booking.Assemblers.BookingAssembler.TEST_DEFAULT_BOOKED_ID;
import static com.lukk.sky.booking.Assemblers.EventAssembler.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("EventSourceServicePrimary unit tests")
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class EventSourceServicePrimaryTest {

    @Mock
    EventSourceRepository eventSourceRepository;

    @InjectMocks
    EventSourceServicePrimary eventSourceServicePrimary;

    Gson gson = new Gson();

    @Test
    @DisplayName("saveEvent assigns sequence number 1 when no prior event exists for the booking")
    void saveEvent_whenNoPriorEventExists_thenSaveWithSequenceNumberOne() {
        try (MockedStatic<Instant> instantMock = Mockito.mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS)) {
            instantMock.when(Instant::now).thenReturn(TEST_DATE);

            Booking booking = BookingAssembler.getPopulatedBooked();
            Event expected = getTestEvent(1, gson.toJson(BookingDTO.of(booking)));

            when(eventSourceRepository.findLastSequenceNumberByBookingId(TEST_DEFAULT_BOOKED_ID))
                    .thenReturn(Optional.empty());

            eventSourceServicePrimary.saveEvent(booking, TEST_EVENT_TYPE);

            ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
            verify(eventSourceRepository, times(1)).save(captor.capture());

            Event saved = captor.getValue();
            assertEquals(expected.getBookingId(), saved.getBookingId());
            assertEquals(expected.getSequenceNumber(), saved.getSequenceNumber());
            assertEquals(expected.getEventType(), saved.getEventType());
            assertEquals(expected.getPayload(), saved.getPayload());
            assertEquals(expected.getTimestamp(), saved.getTimestamp());
        }
    }

    @Test
    @DisplayName("saveEvent increments sequence number when a prior event already exists for the booking")
    void saveEvent_whenPriorEventExists_thenSaveWithIncrementedSequenceNumber() {
        try (MockedStatic<Instant> instantMock = Mockito.mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS)) {
            instantMock.when(Instant::now).thenReturn(TEST_DATE);

            Booking booking = BookingAssembler.getPopulatedBooked();
            Event expected = getTestEvent(3, gson.toJson(BookingDTO.of(booking)));

            when(eventSourceRepository.findLastSequenceNumberByBookingId(TEST_DEFAULT_BOOKED_ID))
                    .thenReturn(Optional.of(2));

            eventSourceServicePrimary.saveEvent(booking, TEST_EVENT_TYPE);

            ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
            verify(eventSourceRepository, times(1)).save(captor.capture());

            Event saved = captor.getValue();
            assertEquals(expected.getBookingId(), saved.getBookingId());
            assertEquals(expected.getSequenceNumber(), saved.getSequenceNumber());
            assertEquals(expected.getEventType(), saved.getEventType());
            assertEquals(expected.getPayload(), saved.getPayload());
            assertEquals(expected.getTimestamp(), saved.getTimestamp());
        }
    }

    @Test
    @DisplayName("saveEvent assigns sequential sequence numbers across two consecutive events for the same booking")
    void saveEvent_whenTwoEventsForSameBooking_thenAssignSequentialNumbers() {
        Booking booking = BookingAssembler.getPopulatedBooked();

        when(eventSourceRepository.findLastSequenceNumberByBookingId(TEST_DEFAULT_BOOKED_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(1));

        eventSourceServicePrimary.saveEvent(booking, TEST_EVENT_TYPE);
        eventSourceServicePrimary.saveEvent(booking, TEST_EVENT_TYPE);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventSourceRepository, times(2)).save(captor.capture());

        List<Event> saved = captor.getAllValues();
        assertEquals(1, saved.get(0).getSequenceNumber());
        assertEquals(2, saved.get(1).getSequenceNumber());
    }

    @Test
    @DisplayName("saveEvent acquires the per-booking advisory lock before reading the sequence and saving")
    void saveEvent_whenSaving_thenLockAcquiredBeforeSequenceReadAndSave() {
        Booking booking = BookingAssembler.getPopulatedBooked();

        when(eventSourceRepository.findLastSequenceNumberByBookingId(TEST_DEFAULT_BOOKED_ID))
                .thenReturn(Optional.of(4));

        eventSourceServicePrimary.saveEvent(booking, TEST_EVENT_TYPE);

        long expectedLockKey = TEST_DEFAULT_BOOKED_ID.getMostSignificantBits() ^ TEST_DEFAULT_BOOKED_ID.getLeastSignificantBits();

        InOrder inOrder = inOrder(eventSourceRepository);
        inOrder.verify(eventSourceRepository).lockBookingEventStream(expectedLockKey);
        inOrder.verify(eventSourceRepository).findLastSequenceNumberByBookingId(TEST_DEFAULT_BOOKED_ID);
        inOrder.verify(eventSourceRepository).save(any(Event.class));
    }

    @Test
    @DisplayName("saveEvent throws IllegalArgumentException and persists nothing when the booking id is null")
    void saveEvent_whenBookingIdIsNull_thenThrowIllegalArgumentException() {
        Booking booking = Booking.builder()
                .offerId(java.util.UUID.randomUUID())
                .bookingUser("user@test.com")
                .ownerEmail("owner@test.com")
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> eventSourceServicePrimary.saveEvent(booking, TEST_EVENT_TYPE));

        verify(eventSourceRepository, never()).save(any(Event.class));
    }
}
