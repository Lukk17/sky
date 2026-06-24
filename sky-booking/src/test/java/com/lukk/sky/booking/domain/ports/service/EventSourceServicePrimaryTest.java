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
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.lukk.sky.booking.Assemblers.BookingAssembler.TEST_DEFAULT_BOOKED_ID;
import static com.lukk.sky.booking.Assemblers.EventAssembler.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

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
    public void saveEvent_whenNoPriorEventExists_thenSaveWithSequenceNumberOne() {
        // testDate need to be initialized now, before mocking LocalDateTime class
        LocalDateTime testDate = TEST_DATE;

        try (MockedStatic<LocalDateTime> localDateTimeMock = Mockito.mockStatic(LocalDateTime.class)) {
            localDateTimeMock.when(LocalDateTime::now).thenReturn(testDate);

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
    public void saveEvent_whenPriorEventExists_thenSaveWithIncrementedSequenceNumber() {
        // GIVEN
        LocalDateTime testDate = TEST_DATE;

        try (MockedStatic<LocalDateTime> localDateTimeMock = Mockito.mockStatic(LocalDateTime.class)) {
            localDateTimeMock.when(LocalDateTime::now).thenReturn(testDate);

            Booking booking = BookingAssembler.getPopulatedBooked();
            Event expected = getTestEvent(3, gson.toJson(BookingDTO.of(booking)));

            when(eventSourceRepository.findLastSequenceNumberByBookingId(TEST_DEFAULT_BOOKED_ID))
                    .thenReturn(Optional.of(2));

            // WHEN
            eventSourceServicePrimary.saveEvent(booking, TEST_EVENT_TYPE);

            // THEN
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
}
