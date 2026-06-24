package com.lukk.sky.offer.domain.ports.service;

import com.lukk.sky.offer.Assemblers.OfferAssembler;
import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.adapters.dto.OfferEditDTO;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.ports.repository.OfferRepository;
import com.lukk.sky.offer.domain.ports.storage.PhotoStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static com.lukk.sky.offer.Assemblers.OfferAssembler.*;
import static com.lukk.sky.offer.Assemblers.UserAssembler.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("OfferServicePrimary — unit tests for the core offer service business logic")
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class OfferServicePrimaryTest {

    @Mock
    OfferRepository offerRepository;

    @Mock
    EventSourceService eventSourceService;

    @Mock
    PhotoStorage photoStorage;

    @InjectMocks
    OfferServicePrimary offerService;

    @Test
    @DisplayName("getAllOffers returns mapped OfferDTO page when offers are present in the repository")
    public void getAllOffers_whenOffersExist_thenReturnMappedOfferDtoPage() {
        //Given
        List<Offer> offers = OfferAssembler.getPopulatedOffers();
        Pageable pageable = PageRequest.of(0, 20);
        when(offerRepository.findAll(pageable)).thenReturn(new PageImpl<>(offers, pageable, offers.size()));

        List<OfferDTO> expected = OfferAssembler.getPopulatedOffersDTO();

        //When
        Page<OfferDTO> actual = offerService.getAllOffers(pageable);

        //Then
        assertEquals(expected, actual.getContent());
        assertEquals(2, actual.getTotalElements());
    }

    @Test
    @DisplayName("getAllOffers returns an empty page when no offers have been saved")
    public void getAllOffers_whenNoOffersSaved_thenReturnEmptyPage() {
        //Given
        Pageable pageable = PageRequest.of(0, 20);
        when(offerRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        //When
        Page<OfferDTO> actual = offerService.getAllOffers(pageable);

        //Then
        assertEquals(0, actual.getTotalElements());
        assertTrue(actual.getContent().isEmpty());
    }

    @Test
    @DisplayName("addOffer saves the offer to the repository and returns the persisted OfferDTO")
    public void addOffer_whenValidOfferDto_thenSaveAndReturnOfferDto() throws OfferException {
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
    @DisplayName("addOffer throws OfferException when an offer with the same ID already exists")
    public void addOffer_whenOfferWithIdAlreadyExists_thenThrowOfferException() throws OfferException {
        //Given
        OfferDTO expected = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);

        when(offerRepository.existsById(TEST_DEFAULT_OFFER_ID)).thenReturn(true);

        //Then
        assertThrows(OfferException.class, () -> {

            //When
            offerService.addOffer(expected);
        });
    }

    @Test
    @DisplayName("deleteOffer removes the offer from the repository when it exists and the requester is the owner")
    public void deleteOffer_whenOfferExistsAndOwnerMatches_thenDeleteFromRepository() {
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
    @DisplayName("deleteOffer throws OfferException when the offer does not exist in the repository")
    public void deleteOffer_whenOfferDoesNotExist_thenThrowOfferException() {
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
    @DisplayName("deleteOffer throws OfferException when the requester email does not match the offer owner")
    public void deleteOffer_whenRequesterIsNotOwner_thenThrowOfferException() {
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
    @DisplayName("getOwnedOffers returns mapped OfferDTO page for offers belonging to the given user")
    public void getOwnedOffers_whenUserHasOffers_thenReturnMappedOfferDtoPage() {
        //Given
        List<Offer> offers = getPopulatedOffers();
        List<OfferDTO> expected = getPopulatedOffersDTO();
        Pageable pageable = PageRequest.of(0, 20);

        when(offerRepository.findAllByOwnerEmail(TEST_USER_EMAIL, pageable))
                .thenReturn(new PageImpl<>(offers, pageable, offers.size()));

        //When
        Page<OfferDTO> actual = offerService.getOwnedOffers(TEST_USER_EMAIL, pageable);

        //Then
        assertEquals(expected, actual.getContent());
        assertEquals(2, actual.getTotalElements());
    }

    @Test
    @DisplayName("getOwnedOffers returns an empty page when the user owns no offers")
    public void getOwnedOffers_whenUserHasNoOffers_thenReturnEmptyPage() {
        //Given
        Pageable pageable = PageRequest.of(0, 20);

        when(offerRepository.findAllByOwnerEmail(TEST_USER_EMAIL, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        //When
        Page<OfferDTO> actual = offerService.getOwnedOffers(TEST_USER_EMAIL, pageable);

        //Then
        assertEquals(0, actual.getTotalElements());
        assertTrue(actual.getContent().isEmpty());
    }

    // problem with JUnit Vintage - not seeing other test when run parametrized...
//    @ParameterizedTest
//    @CsvSource({"testHotelName,2", "testUser@user,2", "testCity,2", "testCountry,2"})
    @Test
    @DisplayName("searchOffers delegates to searchByTerm and returns matching offers for hotel name, owner email, city, and country")
    public void searchOffers_whenSearchTermMatchesOfferFields_thenReturnMatchingOffers() {
        // The id-substring case ("99" → 1) is intentionally omitted: that behaviour relied on
        // in-memory String.valueOf(id).contains(term) filtering which is replaced by a DB LIKE
        // query across text columns only.
        Map<String, Integer> map = new HashMap<>();
        map.put("testHotelName", 2);
        map.put("test@owner.com", 2);
        map.put("testCity", 2);
        map.put("testCountry", 2);

        List<Offer> twoMatches = OfferAssembler.getPopulatedOffers();

        map.forEach((input, expected) -> {

//Given
            when(offerRepository.searchByTerm(input)).thenReturn(twoMatches);

//When
            List<OfferDTO> actual = offerService.searchOffers(input);

//Then
            assertEquals(expected, actual.size());
        });
    }

    @Test
    @DisplayName("editOffer updates fields, saves to the repository, and returns the updated OfferDTO")
    public void editOffer_whenOfferExists_thenSaveAndReturnUpdatedOfferDto() {
        //Given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        OfferEditDTO input = OfferAssembler.getPopulatedOfferEditDTO(TEST_DEFAULT_OFFER_ID);
        OfferDTO expected = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);

        when(offerRepository.findById(offer.getId())).thenReturn(Optional.of(offer));
        when(offerRepository.save(any())).thenReturn(offer);
        doNothing().when(eventSourceService).saveEvent(any(), any());

        //When
        OfferDTO actual = offerService.editOffer(input);

        //Then
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("editOffer throws OfferException when the edit DTO has no ID and the offer is not found")
    public void editOffer_whenOfferIdIsNull_thenThrowOfferException() {
        //Given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        offer.setId(null);

        OfferEditDTO expected = OfferAssembler.getPopulatedOfferEditDTO(TEST_DEFAULT_OFFER_ID);
        expected.setId(null);

        when(offerRepository.findById(offer.getId())).thenReturn(Optional.empty());

        //Then
        assertThrows(OfferException.class, () -> {

            //When
            offerService.editOffer(expected);
        });
    }

    @Test
    @DisplayName("findOfferOwner returns the owner email when the offer exists in the repository")
    public void findOfferOwner_whenOfferExists_thenReturnOwnerEmail() {
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
    @DisplayName("findOfferOwner throws OfferException when no offer with the given ID exists")
    public void findOfferOwner_whenOfferDoesNotExist_thenThrowOfferException() {
        //Given
        when(offerRepository.findById(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.empty());

        //Then
        assertThrows(OfferException.class, () -> {

            //When
            offerService.findOfferOwner(TEST_DEFAULT_OFFER_ID.toString());
        });
    }

    @Test
    @DisplayName("uploadPhoto stores the photo key on the offer and returns updated OfferDTO when caller is the owner")
    public void uploadPhoto_whenCallerIsOwner_thenSetPhotoPathAndReturnUpdatedDto() {
        //Given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        String expectedKey = "offers/test-uuid-hotel.jpg";
        String expectedUrl = "http://localhost:9000/sky-offers-test/" + expectedKey;

        when(offerRepository.findById(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.of(offer));
        when(offerRepository.save(any())).thenReturn(offer);
        when(photoStorage.upload(any(byte[].class), eq("image/jpeg"), eq("hotel.jpg")))
                .thenReturn(expectedKey);
        when(photoStorage.presignedUrl(expectedKey)).thenReturn(expectedUrl);

        //When
        OfferDTO actual = offerService.uploadPhoto(
                TEST_DEFAULT_OFFER_ID, TEST_OWNER_EMAIL, "bytes".getBytes(), "image/jpeg", "hotel.jpg"
        );

        //Then
        assertEquals(expectedKey, actual.getPhotoPath());
        assertEquals(expectedUrl, actual.getPhotoUrl());
        verify(photoStorage).upload(any(byte[].class), eq("image/jpeg"), eq("hotel.jpg"));
        verify(offerRepository).save(any(Offer.class));
    }

    @Test
    @DisplayName("uploadPhoto throws OfferException when the offer does not exist")
    public void uploadPhoto_whenOfferDoesNotExist_thenThrowOfferException() {
        //Given
        when(offerRepository.findById(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.empty());

        //Then
        assertThrows(OfferException.class, () ->
                offerService.uploadPhoto(TEST_DEFAULT_OFFER_ID, TEST_OWNER_EMAIL, new byte[0], "image/jpeg", "f.jpg")
        );
    }

    @Test
    @DisplayName("uploadPhoto throws OfferException when the caller is not the offer owner")
    public void uploadPhoto_whenCallerIsNotOwner_thenThrowOfferException() {
        //Given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        when(offerRepository.findById(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.of(offer));

        //Then
        assertThrows(OfferException.class, () ->
                offerService.uploadPhoto(TEST_DEFAULT_OFFER_ID, SECOND_TEST_USER_EMAIL, new byte[0], "image/jpeg", "f.jpg")
        );
    }
}
