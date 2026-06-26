package com.lukk.sky.booking.domain.ports.service;

import com.lukk.sky.booking.Assemblers.BookingAssembler;
import com.lukk.sky.booking.domain.exception.BookingException;
import com.lukk.sky.booking.domain.model.Booking;
import com.lukk.sky.booking.domain.model.EventType;
import com.lukk.sky.booking.domain.ports.repository.BookingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static com.lukk.sky.booking.Assemblers.BookingAssembler.TEST_DATE;
import static com.lukk.sky.booking.Assemblers.BookingAssembler.getPopulatedBooked;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("BookingPersister unit tests")
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class BookingPersisterTest {

    @Mock
    BookingRepository bookingRepository;

    @Mock
    EventSourceService eventSourceService;

    @InjectMocks
    BookingPersister bookingPersister;

    @Test
    @DisplayName("saveAndPublish persists the booking and publishes the BOOKED event when no conflict exists")
    void saveAndPublish_whenNoConflict_thenPersistAndPublishEvent() {
        Booking booking = getPopulatedBooked();
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        Booking result = bookingPersister.saveAndPublish(booking, List.of(), TEST_DATE);

        verify(bookingRepository).save(booking);
        verify(eventSourceService).saveEvent(booking, EventType.BOOKED);
        assertEquals(booking, result);
    }

    @Test
    @DisplayName("saveAndPublish throws BookingException when the offer is already booked on that date")
    void saveAndPublish_whenOfferAlreadyBookedOnDate_thenThrowBookingException() {
        Booking existing = getPopulatedBooked();
        Booking newBooking = BookingAssembler.getPopulatedBooked();

        assertThrows(BookingException.class,
                () -> bookingPersister.saveAndPublish(newBooking, List.of(existing), TEST_DATE));

        verifyNoInteractions(bookingRepository);
        verifyNoInteractions(eventSourceService);
    }

    @Test
    @DisplayName("saveAndPublish allows booking when existing bookings are for different dates")
    void saveAndPublish_whenExistingBookingsAreForDifferentDates_thenPersistSuccessfully() {
        Booking existingOnDifferentDate = Booking.builder()
                .id(java.util.UUID.randomUUID())
                .offerId(java.util.UUID.randomUUID())
                .bookedDate(TEST_DATE.minusDays(1))
                .bookingUser("other@user.com")
                .ownerEmail("owner@test.com")
                .build();
        Booking newBooking = getPopulatedBooked();
        when(bookingRepository.save(any(Booking.class))).thenReturn(newBooking);

        Booking result = bookingPersister.saveAndPublish(newBooking, List.of(existingOnDifferentDate), TEST_DATE);

        verify(bookingRepository).save(newBooking);
        verify(eventSourceService).saveEvent(newBooking, EventType.BOOKED);
        assertEquals(newBooking, result);
    }
}
