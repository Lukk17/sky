package com.lukk.sky.booking.domain.service;

import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.assemblers.BookingAssembler;
import com.lukk.sky.booking.domain.exception.BookingAccessDeniedException;
import com.lukk.sky.booking.domain.exception.BookingException;
import com.lukk.sky.booking.domain.exception.BookingNotFoundException;
import com.lukk.sky.booking.domain.model.Booking;
import com.lukk.sky.booking.domain.ports.inbound.BookingView;
import com.lukk.sky.booking.domain.ports.outbound.BookingNotificationService;
import com.lukk.sky.booking.domain.ports.outbound.BookingRepository;
import com.lukk.sky.booking.domain.ports.outbound.RestClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
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

import static com.lukk.sky.booking.assemblers.BookingAssembler.TEST_DATE;
import static com.lukk.sky.booking.assemblers.BookingAssembler.TEST_DEFAULT_BOOKED_ID;
import static com.lukk.sky.booking.assemblers.BookingAssembler.getPopulatedBooked;
import static com.lukk.sky.booking.assemblers.BookingAssembler.getPopulatedBookedDTO;
import static com.lukk.sky.booking.assemblers.BookingAssembler.getPopulatedBookedDTOList;
import static com.lukk.sky.booking.assemblers.BookingAssembler.getPopulatedBookedList;
import static com.lukk.sky.common.test.TestUsers.TEST_OWNER_EMAIL;
import static com.lukk.sky.common.test.TestUsers.TEST_USER_EMAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("BookingServicePrimary unit tests")
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class BookingServicePrimaryTest {

    @Mock
    BookingRepository bookingRepository;

    @Mock
    BookingPersister bookingPersister;

    @Mock
    RestClient restClient;

    @Mock
    BookingNotificationService bookingNotificationService;

    @Mock
    java.time.Clock clock;

    @InjectMocks
    BookingServicePrimary bookingService;

    @org.junit.jupiter.api.BeforeEach
    void stubFixedClock() {
        java.time.Clock fixed = java.time.Clock.fixed(
                java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneId.of("UTC"));
        org.mockito.Mockito.lenient().when(clock.instant()).thenReturn(fixed.instant());
        org.mockito.Mockito.lenient().when(clock.getZone()).thenReturn(fixed.getZone());
    }

    @Test
    @DisplayName("getBookedOffersForUser returns both DTOs in a page when user has two bookings")
    void getBookedOffersForUser_whenUserHasTwoBookings_thenReturnBothDtosInPage() {
        // given
        List<Booking> bookings = getPopulatedBookedList();
        List<BookingDTO> expected = getPopulatedBookedDTOList();
        Pageable pageable = PageRequest.of(0, 20);
        when(bookingRepository.findAllByBookingUser(TEST_USER_EMAIL, pageable))
                .thenReturn(new PageImpl<>(bookings, pageable, bookings.size()));

        // when
        Page<BookingView> actual = bookingService.getBookedOffersForUser(TEST_USER_EMAIL, pageable);

        // then
        assertEquals(2, actual.getTotalElements());
        expected.get(0).setId(actual.getContent().get(0).id());
        expected.get(1).setId(actual.getContent().get(1).id());
        assertEquals(expected, actual.getContent().stream().map(BookingAssembler::toBookingDTO).toList());
    }

    @Test
    @DisplayName("bookOffer persists booking, publishes event, and resolves owner when offer and date are valid")
    void bookOffer_whenValidOfferAndDate_thenPersistAndPublishEventAndResolveOwner() {
        // given
        Booking booking = getPopulatedBooked();
        BookingDTO bookingDTO = getPopulatedBookedDTO();
        when(restClient.requestOfferOwner(booking.getOfferId())).thenReturn(TEST_OWNER_EMAIL);
        when(bookingPersister.saveAndPublish(any(), any(), any())).thenReturn(booking);

        // when
        BookingView actual = bookingService.bookOffer(booking.getOfferId(), TEST_DATE, booking.getBookingUser());

        // then
        verify(restClient).requestOfferOwner(booking.getOfferId());
        verify(bookingPersister).saveAndPublish(any(), any(), eq(TEST_DATE));
        bookingDTO.setId(actual.id());
        assertEquals(bookingDTO, BookingAssembler.toBookingDTO(actual));
    }

    @Test
    @DisplayName("bookOffer resolves owner before the transactional boundary so the REST call runs without a DB connection")
    void bookOffer_whenCalled_thenRestCallPrecedesPersisterCall() {
        // given
        Booking booking = getPopulatedBooked();
        when(restClient.requestOfferOwner(booking.getOfferId())).thenReturn(TEST_OWNER_EMAIL);
        when(bookingPersister.saveAndPublish(any(), any(), any())).thenReturn(booking);

        // when
        bookingService.bookOffer(booking.getOfferId(), TEST_DATE, booking.getBookingUser());

        // then
        org.mockito.InOrder order = inOrder(restClient, bookingPersister);
        order.verify(restClient).requestOfferOwner(booking.getOfferId());
        order.verify(bookingPersister).saveAndPublish(any(), any(), any());
    }

    @Test
    @DisplayName("getBookedOffersForUser returns empty page when user has no bookings")
    void getBookedOffersForUser_whenNoBookingsExist_thenReturnEmptyPage() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        when(bookingRepository.findAllByBookingUser(TEST_USER_EMAIL, pageable))
                .thenReturn(new PageImpl<>(new ArrayList<>(), pageable, 0));

        // when
        Page<BookingView> actual = bookingService.getBookedOffersForUser(TEST_USER_EMAIL, pageable);

        // then
        assertEquals(0, actual.getTotalElements());
        assertTrue(actual.getContent().isEmpty());
    }

    @Test
    @DisplayName("bookOffer propagates BookingDateAlreadyBookedException from the persister when the offer is already booked on that date")
    void bookOffer_whenOfferAlreadyBookedOnDate_thenPropagateBookingDateAlreadyBookedException() {
        // given
        Booking booking = getPopulatedBooked();
        when(restClient.requestOfferOwner(booking.getOfferId())).thenReturn(TEST_OWNER_EMAIL);
        when(bookingPersister.saveAndPublish(any(), any(), any()))
                .thenThrow(new com.lukk.sky.booking.domain.exception.BookingDateAlreadyBookedException("already booked"));

        // when / then
        assertThrows(com.lukk.sky.booking.domain.exception.BookingDateAlreadyBookedException.class, () ->
                bookingService.bookOffer(booking.getOfferId(), TEST_DATE, booking.getBookingUser())
        );
        verifyNoInteractions(bookingNotificationService);
    }

    @Test
    @DisplayName("bookOffer throws BookingException when the requested date is in the past")
    void bookOffer_whenDateIsInPast_thenThrowBookingException() {
        // given
        Booking booking = getPopulatedBooked();
        when(restClient.requestOfferOwner(booking.getOfferId())).thenReturn(TEST_OWNER_EMAIL);

        // when / then
        assertThrows(BookingException.class, () ->
                bookingService.bookOffer(booking.getOfferId(), LocalDate.of(1201, 6, 20),
                        booking.getBookingUser())
        );
        verifyNoInteractions(bookingNotificationService);
    }

    @Test
    @DisplayName("removeBooking deletes the booking and announces the removal when called by the booking user")
    void removeBooking_whenCalledByBookingUser_thenDeleteAndAnnounceIt() {
        // given
        Booking booking = getPopulatedBooked();
        when(bookingRepository.findById(TEST_DEFAULT_BOOKED_ID)).thenReturn(Optional.of(booking));

        // when
        bookingService.removeBooking(TEST_DEFAULT_BOOKED_ID, TEST_USER_EMAIL);

        // then
        InOrder order = inOrder(bookingRepository, bookingNotificationService);
        order.verify(bookingRepository, times(1)).delete(booking);
        order.verify(bookingNotificationService).publishRemoved("Booking removed by user", TEST_USER_EMAIL);
    }

    @Test
    @DisplayName("removeBooking deletes the booking and announces the removal when called by the offer owner")
    void removeBooking_whenCalledByOwner_thenDeleteAndAnnounceIt() {
        // given
        Booking booking = getPopulatedBooked();
        when(bookingRepository.findById(TEST_DEFAULT_BOOKED_ID)).thenReturn(Optional.of(booking));

        // when
        bookingService.removeBooking(TEST_DEFAULT_BOOKED_ID, TEST_OWNER_EMAIL);

        // then
        InOrder order = inOrder(bookingRepository, bookingNotificationService);
        order.verify(bookingRepository, times(1)).delete(booking);
        order.verify(bookingNotificationService).publishRemoved("Booking removed by owner", TEST_OWNER_EMAIL);
    }

    @Test
    @DisplayName("removeBooking throws BookingAccessDeniedException when called by a user who is neither the booker nor the owner")
    void removeBooking_whenCalledByUnrelatedUser_thenThrowBookingAccessDeniedException() {
        // given
        Booking booking = getPopulatedBooked();
        when(bookingRepository.findById(TEST_DEFAULT_BOOKED_ID)).thenReturn(Optional.of(booking));

        // when / then
        assertThrows(BookingAccessDeniedException.class, () ->
                bookingService.removeBooking(TEST_DEFAULT_BOOKED_ID, "other@user.com")
        );
        verifyNoInteractions(bookingNotificationService);
    }

    @Test
    @DisplayName("removeBooking throws BookingNotFoundException when the booking does not exist")
    void removeBooking_whenBookingDoesNotExist_thenThrowBookingNotFoundException() {
        // given
        when(bookingRepository.findById(TEST_DEFAULT_BOOKED_ID)).thenReturn(Optional.empty());

        // when / then
        assertThrows(BookingNotFoundException.class, () ->
                bookingService.removeBooking(TEST_DEFAULT_BOOKED_ID, TEST_USER_EMAIL)
        );
        verifyNoInteractions(bookingNotificationService);
    }

    @Test
    @DisplayName("bookOffer publishes the booking only after the persister has committed it")
    void bookOffer_whenBookingIsPersisted_thenPublishCreatedAfterThePersisterCall() {
        // given
        Booking booking = getPopulatedBooked();
        when(restClient.requestOfferOwner(booking.getOfferId())).thenReturn(TEST_OWNER_EMAIL);
        when(bookingPersister.saveAndPublish(any(), any(), any())).thenReturn(booking);

        // when
        BookingView actual = bookingService.bookOffer(booking.getOfferId(), TEST_DATE, TEST_USER_EMAIL);

        // then
        InOrder order = inOrder(bookingPersister, bookingNotificationService);
        order.verify(bookingPersister).saveAndPublish(any(), any(), eq(TEST_DATE));
        order.verify(bookingNotificationService).publishCreated(actual, TEST_USER_EMAIL);
    }
}
