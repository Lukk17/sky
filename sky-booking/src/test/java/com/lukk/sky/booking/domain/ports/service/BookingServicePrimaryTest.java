package com.lukk.sky.booking.domain.ports.service;

import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.domain.exception.BookingException;
import com.lukk.sky.booking.domain.model.Booking;
import com.lukk.sky.booking.domain.ports.api.RestClient;
import com.lukk.sky.booking.domain.ports.repository.BookingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.lukk.sky.booking.Assemblers.BookingAssembler.*;
import static com.lukk.sky.booking.Assemblers.UserAssembler.TEST_OWNER_EMAIL;
import static com.lukk.sky.booking.Assemblers.UserAssembler.TEST_USER_EMAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("BookingServicePrimary unit tests")
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class BookingServicePrimaryTest {

    @Mock
    BookingRepository bookingRepository;

    @Mock
    BookingPersister bookingPersister;

    @Mock
    RestClient restClient;

    @InjectMocks
    BookingServicePrimary bookingService;

    @Test
    @DisplayName("getBookedOffersForUser returns both DTOs in a page when user has two bookings")
    public void getBookedOffersForUser_whenUserHasTwoBookings_thenReturnBothDtosInPage() {
        // Given
        List<Booking> bookings = getPopulatedBookedList();
        List<BookingDTO> expected = getPopulatedBookedDTOList();
        Pageable pageable = PageRequest.of(0, 20);

        when(bookingRepository.findAllByBookingUser(TEST_USER_EMAIL, pageable))
                .thenReturn(new PageImpl<>(bookings, pageable, bookings.size()));

        // When
        Page<BookingDTO> actual = bookingService.getBookedOffersForUser(TEST_USER_EMAIL, pageable);

        // Then
        assertEquals(2, actual.getTotalElements());
        expected.get(0).setId(actual.getContent().get(0).getId());
        expected.get(1).setId(actual.getContent().get(1).getId());
        assertEquals(expected, actual.getContent());
    }

    @Test
    @DisplayName("bookOffer persists booking, publishes event, and resolves owner when offer and date are valid")
    public void bookOffer_whenValidOfferAndDate_thenPersistAndPublishEventAndResolveOwner() {
        // Given
        Booking booking = getPopulatedBooked();
        BookingDTO bookingDTO = getPopulatedBookedDTO();

        when(restClient.requestOfferOwner(booking.getOfferId())).thenReturn(TEST_OWNER_EMAIL);
        when(bookingPersister.saveAndPublish(any(), any(), any())).thenReturn(booking);

        // When
        BookingDTO actual = bookingService.bookOffer(booking.getOfferId(), TEST_DATE.toString(),
                booking.getBookingUser());

        // Then
        verify(restClient).requestOfferOwner(booking.getOfferId());
        verify(bookingPersister).saveAndPublish(any(), any(), eq(TEST_DATE));
        bookingDTO.setId(actual.getId());
        assertEquals(bookingDTO, actual);
    }

    @Test
    @DisplayName("bookOffer resolves owner before the transactional boundary so the REST call runs without a DB connection")
    public void bookOffer_whenCalled_thenRestCallPrecedesPersisterCall() {
        // Given
        Booking booking = getPopulatedBooked();

        when(restClient.requestOfferOwner(booking.getOfferId())).thenReturn(TEST_OWNER_EMAIL);
        when(bookingPersister.saveAndPublish(any(), any(), any())).thenReturn(booking);

        // When
        bookingService.bookOffer(booking.getOfferId(), TEST_DATE.toString(), booking.getBookingUser());

        // Then
        org.mockito.InOrder order = inOrder(restClient, bookingPersister);
        order.verify(restClient).requestOfferOwner(booking.getOfferId());
        order.verify(bookingPersister).saveAndPublish(any(), any(), any());
    }

    @Test
    @DisplayName("getBookedOffersForUser returns empty page when user has no bookings")
    public void getBookedOffersForUser_whenNoBookingsExist_thenReturnEmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);

        when(bookingRepository.findAllByBookingUser(TEST_USER_EMAIL, pageable))
                .thenReturn(new PageImpl<>(new ArrayList<>(), pageable, 0));

        // When
        Page<BookingDTO> actual = bookingService.getBookedOffersForUser(TEST_USER_EMAIL, pageable);

        // Then
        assertEquals(0, actual.getTotalElements());
        assertTrue(actual.getContent().isEmpty());
    }

    @Test
    @DisplayName("bookOffer propagates BookingException from the persister when the offer is already booked on that date")
    public void bookOffer_whenOfferAlreadyBookedOnDate_thenPropagateBookingException() {
        // Given
        Booking booking = getPopulatedBooked();
        when(restClient.requestOfferOwner(booking.getOfferId())).thenReturn(TEST_OWNER_EMAIL);
        when(bookingPersister.saveAndPublish(any(), any(), any()))
                .thenThrow(new com.lukk.sky.booking.domain.exception.BookingException("already booked"));

        // Then
        assertThrows(com.lukk.sky.booking.domain.exception.BookingException.class, () ->
                // When
                bookingService.bookOffer(booking.getOfferId(), TEST_DATE.toString(), booking.getBookingUser())
        );
    }

    @Test
    @DisplayName("bookOffer throws BookingException when the requested date is in the past")
    public void bookOffer_whenDateIsInPast_thenThrowBookingException() {
        // Given
        Booking booking = getPopulatedBooked();
        when(restClient.requestOfferOwner(booking.getOfferId())).thenReturn(TEST_OWNER_EMAIL);

        // Then
        assertThrows(BookingException.class, () ->
                // When
                bookingService.bookOffer(booking.getOfferId(), LocalDate.of(1201, 6, 20).toString(),
                        booking.getBookingUser())
        );
    }

    @Test
    @DisplayName("removeBooking deletes the booking and returns confirmation when called by the booking user")
    public void removeBooking_whenCalledByBookingUser_thenDeleteAndReturnMessage() {
        // Given
        String expected = "Booking removed by user";
        Booking booking = getPopulatedBooked();
        when(bookingRepository.findById(TEST_DEFAULT_BOOKED_ID)).thenReturn(Optional.of(booking));

        // When
        String actual = bookingService.removeBooking(TEST_DEFAULT_BOOKED_ID.toString(), TEST_USER_EMAIL);

        // Then
        verify(bookingRepository, times(1)).delete(booking);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("removeBooking deletes the booking and returns confirmation when called by the offer owner")
    public void removeBooking_whenCalledByOwner_thenDeleteAndReturnMessage() {
        // Given
        String expected = "Booking removed by owner";
        Booking booking = getPopulatedBooked();
        when(bookingRepository.findById(TEST_DEFAULT_BOOKED_ID)).thenReturn(Optional.of(booking));

        // When
        String actual = bookingService.removeBooking(TEST_DEFAULT_BOOKED_ID.toString(), TEST_OWNER_EMAIL);

        // Then
        verify(bookingRepository, times(1)).delete(booking);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("removeBooking throws BookingException when called by a user who is neither the booker nor the owner")
    public void removeBooking_whenCalledByUnrelatedUser_thenThrowBookingException() {
        // Given
        Booking booking = getPopulatedBooked();
        when(bookingRepository.findById(TEST_DEFAULT_BOOKED_ID)).thenReturn(Optional.of(booking));

        // Then
        assertThrows(BookingException.class, () ->
                // When
                bookingService.removeBooking(TEST_DEFAULT_BOOKED_ID.toString(), "other@user.com")
        );
    }

    @Test
    @DisplayName("removeBooking throws BookingException when the booking does not exist")
    public void removeBooking_whenBookingDoesNotExist_thenThrowBookingException() {
        // Given
        when(bookingRepository.findById(TEST_DEFAULT_BOOKED_ID)).thenReturn(Optional.empty());

        // Then
        assertThrows(BookingException.class, () ->
                // When
                bookingService.removeBooking(TEST_DEFAULT_BOOKED_ID.toString(), TEST_USER_EMAIL)
        );
    }
}
