package com.lukk.sky.offer.domain.ports.service;

import com.lukk.sky.offer.Assemblers.OfferAssembler;
import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.ports.repository.OfferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static com.lukk.sky.offer.Assemblers.OfferAssembler.*;
import static com.lukk.sky.offer.Assemblers.UserAssembler.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class OfferServicePrimaryTest {

    @Mock
    OfferRepository offerRepository;

    @Mock
    EventSourceService eventSourceService;

    @InjectMocks
    OfferServicePrimary offerService;

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
        doNothing().when(eventSourceService).saveEvent(any(), any());

        //When
        OfferDTO actual = offerService.addOffer(expected);

        //Then
        assertEquals(expected, actual);
    }

    @Test
    // This scenario should not happen
    public void whenAddExistingOffer_thenThrowException() throws OfferException {
        //Given
        OfferDTO expected = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);

        when(offerRepository.findById(any())).thenReturn(Optional.of(offer));

        //Then
        assertThrows(OfferException.class, () -> {

            //When
            offerService.addOffer(expected);
        });
    }

    @Test
    public void whenDeleteOffer_thenDeleteOffer() {
        //Given
        Offer expected = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        ArgumentCaptor<Offer> valueCapture = ArgumentCaptor.forClass(Offer.class);

        doReturn(Optional.of(expected)).when(offerRepository).findById(expected.getId());
        doNothing().when(offerRepository).delete(valueCapture.capture());
        doNothing().when(eventSourceService).saveEvent(any(), any());

        //When
        offerService.deleteOffer(TEST_DEFAULT_OFFER_ID, TEST_OWNER_EMAIL);

        //Then
        assertEquals(expected, valueCapture.getValue());
    }

    @Test
    public void whenDeleteNotExistingOffer_thenThrowException() {
        //Given
        Offer expected = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);

        doReturn(Optional.empty()).when(offerRepository).findById(expected.getId());

        //Then
        assertThrows(OfferException.class, () -> {

            //When
            offerService.deleteOffer(TEST_DEFAULT_OFFER_ID, TEST_USER_EMAIL);
        });
    }

    @Test
    public void whenDeleteNotOwnedOffer_thenThrowException() {
        //Given
        Offer expected = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);

        doReturn(Optional.of(expected)).when(offerRepository).findById(expected.getId());

        //Then
        assertThrows(OfferException.class, () -> {

            //When
            offerService.deleteOffer(TEST_DEFAULT_OFFER_ID, SECOND_TEST_USER_EMAIL);
        });
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
        map.put("test@owner.com", 2);
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
        when(offerRepository.save(any())).thenReturn(offer);
        doNothing().when(eventSourceService).saveEvent(any(), any());

        //When
        OfferDTO actual = offerService.editOffer(expected);

        //Then
        assertEquals(expected, actual);
    }

    @Test
    public void whenEditOfferWithoutID_then() {
        //Given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        offer.setId(null);

        OfferDTO expected = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        expected.setId(null);

        when(offerRepository.findById(offer.getId())).thenReturn(Optional.empty());

        //Then
        assertThrows(OfferException.class, () -> {

            //When
            offerService.editOffer(expected);
        });
    }

    @Test
    public void whenFindOfferOwner_thenReturnOwnerId() {
        //Given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        OfferDTO expected = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);

        when(offerRepository.findById(offer.getId())).thenReturn(Optional.of(offer));

        //When
        String actual = offerService.findOfferOwner(TEST_DEFAULT_OFFER_ID.toString());

        //Then
        assertEquals(TEST_OWNER_EMAIL, actual);
    }

    @Test
    public void whenFindOwner_ofNonExistingOffer_thenThrowError() {
        //Given
        when(offerRepository.findById(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.empty());

        //Then
        assertThrows(OfferException.class, () -> {

            //When
            offerService.findOfferOwner(TEST_DEFAULT_OFFER_ID.toString());
        });
    }
}
