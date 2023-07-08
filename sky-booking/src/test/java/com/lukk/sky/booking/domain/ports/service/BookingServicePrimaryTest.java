package com.lukk.sky.booking.domain.ports.service;

import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.domain.exception.BookingException;
import com.lukk.sky.booking.domain.model.Booking;
import com.lukk.sky.booking.domain.ports.api.RestClient;
import com.lukk.sky.booking.domain.ports.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.lukk.sky.booking.Assemblers.BookingAssembler.*;
import static com.lukk.sky.booking.Assemblers.UserAssembler.TEST_USER_EMAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class BookingServicePrimaryTest {

    @Mock
    BookingRepository bookingRepository;

    @Mock
    EventSourceService eventSourceService;

    @Mock
    RestClient restClient;

    @InjectMocks
    BookingServicePrimary bookingService;

    @Test
    public void whenFindAllByUser_thenResultAllUserBooked() {
        //Given
        List<Booking> bookings = getPopulatedBookedList();
        List<BookingDTO> expected = getPopulatedBookedDTOList();

        when(bookingRepository.findAllByBookingUser(TEST_USER_EMAIL)).thenReturn(bookings);

        //When
        List<BookingDTO> actual = bookingService.getBookedOffersForUser(TEST_USER_EMAIL);

        //Then
        assertEquals(expected, actual);
    }

    @Test
    public void whenBookOffer_thenAddBooked() {
        //Given
        Booking booking = getPopulatedBooked();
        BookingDTO bookingDTO = getPopulatedBookedDTO();

        // any() because booking id is removed when saving to DB (DB normally autogenerate it)
        when(bookingRepository.save(any())).thenReturn(booking);
        when(restClient.requestOfferOwner(booking.getOfferId())).thenReturn(Mono.just(booking.getOfferId()));
        doNothing().when(eventSourceService).saveEvent(any(), any());

        //When
        BookingDTO actual = bookingService.bookOffer(booking.getOfferId(), TEST_DATE.toString(),
                booking.getBookingUser()).block();

        //Then
        assertEquals(bookingDTO, actual);
    }

    @Test
    public void whenGetBookedOffersNotExist_thenReturnEmptyList() {
        //Given
        List<BookingDTO> expected = new ArrayList<>();

        when(bookingRepository.findAllByBookingUser(TEST_USER_EMAIL)).thenReturn(new ArrayList<>());

        //When
        List<BookingDTO> actual = bookingService.getBookedOffersForUser(TEST_USER_EMAIL);

        //Then
        assertEquals(expected, actual);
    }


    @Test
    public void whenBookOfferAlreadyBooked_thenThrowException() {
        //Given
        List<Booking> booked = getPopulatedBookedList();
        when(bookingRepository.findAllByOfferId(TEST_DEFAULT_OFFER_ID)).thenReturn(booked);

        //Then
        assertThrows(BookingException.class, () -> {

            //When
            bookingService.bookOffer(TEST_DEFAULT_OFFER_ID, TEST_DATE.toString(),
                    TEST_USER_EMAIL);
        });
    }

    @Test
    public void whenBookOfferInPast_thenThrowException() {
        //Given
        Booking booking = getPopulatedBooked();

        //Then
        assertThrows(BookingException.class, () -> {

            //When
            bookingService.bookOffer(booking.getOfferId(), LocalDate.of(1201, 6, 20).toString(),
                    booking.getBookingUser());
        });
    }
}
