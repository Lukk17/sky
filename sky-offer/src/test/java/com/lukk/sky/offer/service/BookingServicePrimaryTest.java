package com.lukk.sky.offer.service;

import com.lukk.sky.offer.Assemblers.BookingAssembler;
import com.lukk.sky.offer.Assemblers.OfferAssembler;
import com.lukk.sky.offer.H2TestProfileJPAConfig;
import com.lukk.sky.offer.SkyOfferApplication;
import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.model.Booked;
import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.ports.repository.BookedRepository;
import com.lukk.sky.offer.domain.ports.service.BookingService;
import com.lukk.sky.offer.domain.ports.service.OfferService;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.lukk.sky.offer.Assemblers.BookingAssembler.*;
import static com.lukk.sky.offer.Assemblers.OfferAssembler.TEST_DEFAULT_OFFER_ID;
import static com.lukk.sky.offer.Assemblers.OfferAssembler.getPopulatedOffersDTO;
import static com.lukk.sky.offer.Assemblers.UserAssembler.TEST_USER_EMAIL;
import static com.lukk.sky.offer.config.Constants.DATE_FORMAT;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
        SkyOfferApplication.class,
        H2TestProfileJPAConfig.class})
@ActiveProfiles("test")
public class BookingServicePrimaryTest {

    @Autowired
    BookingService bookingService;

    @MockBean
    BookedRepository bookedRepository;

    @MockBean
    OfferService offerService;

    @Test
    public void whenFindAllByUser_thenResultAllUserBooked() {
        //Given
        List<Booked> expected = getEmptyBookedList();
        when(bookedRepository.findAllByUserEmail(TEST_USER_EMAIL)).thenReturn(expected);

        //When
        List<Booked> actual = bookingService.findAllByUser(TEST_USER_EMAIL);

        //Then
        assertEquals(expected, actual);
    }

    @Test
    public void whenAddBooked_thenAddBooked() {
        //Given
        Booked expected = getPopulatedBooked(TEST_USER_EMAIL, 1L);

        when(bookedRepository.save(expected)).thenReturn(expected);

        //When
        Booked actual = bookingService.addBooked(expected);

        //Then
        assertEquals(expected, actual);
    }

    @Test
    public void whenGetBookedOffers_thenReturnList() {
        //Given
        List<OfferDTO> expected = List.of(getPopulatedOffersDTO().get(0));
        List<Booked> bookedList = BookingAssembler.getPopulatedBookedList_withPopulatedOffers();

        when(bookingService.findAllByUser(TEST_USER_EMAIL)).thenReturn(bookedList);

        //When
        List<OfferDTO> actual = bookingService.getBookedOffers(TEST_USER_EMAIL);

        //Then
        Assertions.assertEquals(expected, actual);
    }


    @Test
    public void whenGetBookedOffersNotExist_thenReturnEmptyList() {
        //Given
        List<OfferDTO> expected = new ArrayList<>();

        when(bookingService.findAllByUser(TEST_USER_EMAIL)).thenReturn(new ArrayList<>());

        //When
        List<OfferDTO> actual = bookingService.getBookedOffers(TEST_USER_EMAIL);

        //Then
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void whenBookOffer_thenSaveBooked() {
        //Given
        // Date format: 2011-12-03 (as declared in OfferServiceImpl.DATE_FORMAT
        LocalDate dateToBook = LocalDate.now();
        String dateToBookSTR = dateToBook.format(DATE_FORMAT);

        Booked booked = BookingAssembler.getPopulatedBooked(TEST_USER_EMAIL, TEST_DEFAULT_BOOKED_ID);
        booked.setBookedDate(dateToBook);

        OfferDTO expected = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        //list have to be mutable ! easiest way to do it with one element:

        when(offerService.getOffer(Long.parseLong(TEST_DEFAULT_OFFER_ID.toString())))
                .thenReturn(Optional.ofNullable(expected.toDomain()));
        when(offerService.editOffer(any())).thenReturn(expected);


        prepareBookingTest(booked);
        expected.setBooked(new ArrayList<>(List.of(booked)));


        //When
        OfferDTO actual = bookingService.bookOffer(TEST_DEFAULT_OFFER_ID.toString(), dateToBookSTR, TEST_USER_EMAIL);

        //Then
        Assertions.assertEquals(expected, actual);

    }

    @Test(expected = OfferException.class)
    public void whenBookOfferAlreadyBooked_thenThrowException() {
        //Given
        // Date format: 2011-12-03 (as declared in OfferServiceImpl.DATE_FORMAT
        LocalDate dateToBook = LocalDate.now();
        String dateToBookSTR = dateToBook.format(DATE_FORMAT);

        Booked booked = BookingAssembler.getPopulatedBooked(TEST_USER_EMAIL, TEST_DEFAULT_BOOKED_ID);
        booked.setBookedDate(dateToBook);

        OfferDTO expected = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        //list have to be mutable ! easiest way to do it with one element:
        expected.setBooked(new ArrayList<>(List.of(booked)));

        Offer offerBeforeBooking = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        offerBeforeBooking.setBooked(new ArrayList<>(List.of(booked)));

        when(offerService.editOffer(expected)).thenReturn(OfferDTO.of(offerBeforeBooking));

        //When
        OfferDTO actual = bookingService.bookOffer(TEST_DEFAULT_OFFER_ID.toString(), dateToBookSTR, TEST_USER_EMAIL);

        //Then
        Assertions.assertEquals(expected, actual);
    }

    @Test(expected = OfferException.class)
    public void whenBookOfferInPast_thenThrowException() {
        //Given
        // Date format: 2011-12-03 (as declared in OfferServiceImpl.DATE_FORMAT
        LocalDate dateToBook = LocalDate.now().minus(1, ChronoUnit.DAYS);
        String dateToBookSTR = dateToBook.format(DATE_FORMAT);

        Booked booked = BookingAssembler.getPopulatedBooked(TEST_USER_EMAIL, TEST_DEFAULT_BOOKED_ID);
        booked.setBookedDate(dateToBook);

        OfferDTO expected = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        //list have to be mutable ! easiest way to do it with one element:
        expected.setBooked(new ArrayList<>(List.of(booked)));

        Offer offerBeforeBooking = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        offerBeforeBooking.setBooked(new ArrayList<>(List.of(booked)));

        when(offerService.editOffer(expected)).thenReturn(OfferDTO.of(offerBeforeBooking));

        //When
        OfferDTO actual = bookingService.bookOffer(TEST_DEFAULT_OFFER_ID.toString(), dateToBookSTR, TEST_USER_EMAIL);

        //Then
        Assertions.assertEquals(expected, actual);
    }

    private void prepareBookingTest(Booked booked) {
        Offer offerBeforeBooking = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        offerBeforeBooking.setBooked(new ArrayList<>());

        Offer offerAfterBooking = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        offerAfterBooking.setBooked(new ArrayList<>(List.of(booked)));

        when(bookingService.addBooked(any())).thenReturn(booked);
    }
}
