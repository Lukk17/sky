package com.lukk.sky.offer.service;

import com.lukk.sky.offer.Assemblers.OfferAssembler;
import com.lukk.sky.offer.H2TestProfileJPAConfig;
import com.lukk.sky.offer.SkyOfferApplication;
import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.ports.repository.OfferRepository;
import com.lukk.sky.offer.domain.ports.service.BookingService;
import com.lukk.sky.offer.domain.ports.service.OfferService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.validation.ValidationException;
import java.util.*;

import static com.lukk.sky.offer.Assemblers.OfferAssembler.*;
import static com.lukk.sky.offer.Assemblers.UserAssembler.SECOND_TEST_USER_EMAIL;
import static com.lukk.sky.offer.Assemblers.UserAssembler.TEST_USER_EMAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
        SkyOfferApplication.class,
        H2TestProfileJPAConfig.class})
@ActiveProfiles("test")
public class OfferServiceImplTest {

    @Autowired
    OfferService offerService;

    @MockBean
    OfferRepository offerRepository;

    @MockBean
    BookingService bookingService;

    @Test
    public void whenGetAllOffers_thenReturnOffers() {
        //Given
        List<Offer> offers = OfferAssembler.getPopulatedOffers();
        when(offerRepository.findAll()).thenReturn(offers);

        List<OfferDTO> expected = OfferAssembler.getPopulatedOffersDTO();

        //When
        List<OfferDTO> actual = offerService.getAllOffers();

        //Then
        assertEquals(expected, actual);
    }

    @Test
    public void whenGetAllOffersAndNoOffersSaved_thenReturnEmptyList() {
        //Given
        List<Offer> offers = new ArrayList<>();
        when(offerRepository.findAll()).thenReturn(offers);

        List<OfferDTO> expected = new ArrayList<>();

        //When
        List<OfferDTO> actual = offerService.getAllOffers();

        //Then
        assertEquals(expected, actual);
    }

    @Test
    public void whenAddOffer_thenReturnAddedOffer() throws OfferException {
        //Given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        OfferDTO expected = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);

        // any() because offer ID is skipped in DTO converter (as repository will assign it when saving to DB)
        // due to that offer sent to repo by service don't have ID field and is not same as this offer
        when(offerRepository.save(any())).thenReturn(offer);

        //When
        OfferDTO actual = offerService.addOffer(expected);

        //Then
        assertEquals(expected, actual);
    }

    @Test(expected = OfferException.class)
    // This scenario should not happen
    public void whenAddExistingOffer_thenThrowException() throws OfferException {
        //Given
        OfferDTO expected = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);

        when(offerRepository.save(any())).thenThrow(new OfferException("Offer with given ID already exist!"));

        //When
        offerService.addOffer(expected);

        //Then - exception is thrown
    }

    @Test
    public void whenDeleteOffer_thenDeleteOffer() {
        //Given
        Offer expected = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        ArgumentCaptor<Offer> valueCapture = ArgumentCaptor.forClass(Offer.class);

        doReturn(Optional.of(expected)).when(offerRepository).findById(expected.getId());
        doNothing().when(offerRepository).delete(valueCapture.capture());

        //When
        offerService.deleteOffer(TEST_DEFAULT_OFFER_ID, TEST_USER_EMAIL);

        //Then
        assertEquals(expected, valueCapture.getValue());
    }

    @Test(expected = OfferException.class)
    public void whenDeleteNotExistingOffer_thenThrowException() {
        //Given
        Offer expected = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);

        doReturn(Optional.empty()).when(offerRepository).findById(expected.getId());

        //When
        offerService.deleteOffer(TEST_DEFAULT_OFFER_ID, TEST_USER_EMAIL);

        //Then - throw exception
    }

    @Test(expected = OfferException.class)
    public void whenDeleteNotOwnedOffer_thenThrowException() {
        //Given
        Offer expected = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);

        doReturn(Optional.of(expected)).when(offerRepository).findById(expected.getId());

        //When
        offerService.deleteOffer(TEST_DEFAULT_OFFER_ID, SECOND_TEST_USER_EMAIL);

        //Then - throw exception
    }

    @Test
    public void whenGetOwnedOffers_thenReturnOffersList() {
        //Given
        List<Offer> offers = getPopulatedOffers();
        List<OfferDTO> expected = getPopulatedOffersDTO();

        when(offerRepository.findAllByOwnerEmail(TEST_USER_EMAIL)).thenReturn(offers);

        //When
        List<OfferDTO> actual = offerService.getOwnedOffers(TEST_USER_EMAIL);

        //Then
        assertEquals(expected, actual);
    }

    @Test
    public void whenGetOwnedOffersNotExist_thenReturnEmptyList() {
        //Given
        List<Offer> offers = new ArrayList<>();
        List<OfferDTO> expected = new ArrayList<>();

        when(offerRepository.findAllByOwnerEmail(TEST_USER_EMAIL)).thenReturn(offers);

        //When
        List<OfferDTO> actual = offerService.getOwnedOffers(TEST_USER_EMAIL);

        //Then
        assertEquals(expected, actual);
    }

    // problem with JUnit Vintage - not seeing other test when run parametrized...
//    @ParameterizedTest
//    @CsvSource({"testHotelName,2", "testUser@user,2", "testCity,2", "testCountry,2", "99,1"})
    @Test
    public void searchOffer() {
        Map<String, Integer> map = new HashMap<>();
        map.put("testHotelName", 2);
        map.put("testUser", 2);
        map.put("testCity", 2);
        map.put("testCountry", 2);
        map.put("99", 1);

        map.forEach((input, expected) -> {

//Given
            long id = 0L;
            try {
                id = Long.parseLong(input);
            } catch (NumberFormatException e) {
                // normal operation for most of CsvSource values
            }
            //  need to convert into mutable list to add element
            List<Offer> offers = new ArrayList<>(OfferAssembler.getPopulatedOffers());
            offers.add(OfferAssembler.getEmptyOffer(id));

            when(offerRepository.findAll()).thenReturn(offers);

//When
            List<OfferDTO> actual = offerService.searchOffers(input);

//Then
            assertEquals(expected, actual.size());
        });

    }

    @Test
    public void whenEditOffer_thenSaveAndReturnOffer() {
        //Given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        OfferDTO expected = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);

        when(offerRepository.findById(offer.getId())).thenReturn(Optional.of(offer));
        when(offerRepository.save(offer)).thenReturn(offer);

        //When
        OfferDTO actual = offerService.editOffer(expected);

        //Then
        assertEquals(expected, actual);
    }

    @Test(expected = ValidationException.class)
    public void whenEditOfferWithoutID_then() {
        //Given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        offer.setId(null);

        OfferDTO expected = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        expected.setId(null);

        when(offerRepository.findById(offer.getId())).thenReturn(Optional.empty());

        //When
        offerService.editOffer(expected);

        //Then - throw exception
    }


}
