package com.lukk.sky.offer.domain.ports.service;

import com.lukk.sky.offer.Assemblers.OfferAssembler;
import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.adapters.dto.OfferEditDTO;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.model.EventType;
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static com.lukk.sky.offer.Assemblers.OfferAssembler.TEST_DEFAULT_OFFER_ID;
import static com.lukk.sky.offer.Assemblers.OfferAssembler.getPopulatedOffers;
import static com.lukk.sky.offer.Assemblers.OfferAssembler.getPopulatedOffersDTO;
import static com.lukk.sky.offer.Assemblers.UserAssembler.SECOND_TEST_USER_EMAIL;
import static com.lukk.sky.offer.Assemblers.UserAssembler.TEST_OWNER_EMAIL;
import static com.lukk.sky.offer.Assemblers.UserAssembler.TEST_USER_EMAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OfferServicePrimary — unit tests for the core offer service business logic")
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class OfferServicePrimaryTest {

    @Mock
    OfferRepository offerRepository;

    @Mock
    EventSourceService eventSourceService;

    @Mock
    PhotoStorage photoStorage;

    @InjectMocks
    OfferServicePrimary offerService;

    @Test
    @DisplayName("getAllOffers_whenOffersExist_thenReturnMappedOfferDtoPage")
    void getAllOffers_whenOffersExist_thenReturnMappedOfferDtoPage() {
        List<Offer> offers = OfferAssembler.getPopulatedOffers();
        Pageable pageable = PageRequest.of(0, 20);
        when(offerRepository.findAll(pageable)).thenReturn(new PageImpl<>(offers, pageable, offers.size()));

        List<OfferDTO> expected = OfferAssembler.getPopulatedOffersDTO();

        Page<OfferDTO> actual = offerService.getAllOffers(pageable);

        assertEquals(expected, actual.getContent());
        assertEquals(2, actual.getTotalElements());
    }

    @Test
    @DisplayName("getAllOffers_whenNoOffersSaved_thenReturnEmptyPage")
    void getAllOffers_whenNoOffersSaved_thenReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 20);
        when(offerRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<OfferDTO> actual = offerService.getAllOffers(pageable);

        assertEquals(0, actual.getTotalElements());
        assertTrue(actual.getContent().isEmpty());
    }

    @Test
    @DisplayName("addOffer_whenValidOfferDto_thenSaveAndReturnOfferDto")
    void addOffer_whenValidOfferDto_thenSaveAndReturnOfferDto() throws OfferException {
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        OfferDTO expected = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);

        when(offerRepository.save(any())).thenReturn(offer);
        doNothing().when(eventSourceService).saveEvent(any(), any());

        OfferDTO actual = offerService.addOffer(expected);

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("addOffer_whenOfferWithIdAlreadyExists_thenThrowOfferException")
    void addOffer_whenOfferWithIdAlreadyExists_thenThrowOfferException() throws OfferException {
        OfferDTO expected = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);

        when(offerRepository.existsById(TEST_DEFAULT_OFFER_ID)).thenReturn(true);

        assertThrows(OfferException.class, () -> {
            offerService.addOffer(expected);
        });
    }

    @Test
    @DisplayName("deleteOffer_whenOfferExistsAndOwnerMatches_thenDeleteFromRepository")
    void deleteOffer_whenOfferExistsAndOwnerMatches_thenDeleteFromRepository() {
        Offer expected = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        ArgumentCaptor<Offer> valueCapture = ArgumentCaptor.forClass(Offer.class);

        doReturn(Optional.of(expected)).when(offerRepository).findById(expected.getId());
        doNothing().when(offerRepository).delete(valueCapture.capture());
        doNothing().when(eventSourceService).saveEvent(any(), any());

        offerService.deleteOffer(TEST_DEFAULT_OFFER_ID, TEST_OWNER_EMAIL);

        assertEquals(expected, valueCapture.getValue());
    }

    @Test
    @DisplayName("deleteOffer_whenOfferDoesNotExist_thenThrowOfferException")
    void deleteOffer_whenOfferDoesNotExist_thenThrowOfferException() {
        Offer expected = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);

        doReturn(Optional.empty()).when(offerRepository).findById(expected.getId());

        assertThrows(OfferException.class, () -> {
            offerService.deleteOffer(TEST_DEFAULT_OFFER_ID, TEST_USER_EMAIL);
        });
    }

    @Test
    @DisplayName("deleteOffer_whenRequesterIsNotOwner_thenThrowOfferException")
    void deleteOffer_whenRequesterIsNotOwner_thenThrowOfferException() {
        Offer expected = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);

        doReturn(Optional.of(expected)).when(offerRepository).findById(expected.getId());

        assertThrows(OfferException.class, () -> {
            offerService.deleteOffer(TEST_DEFAULT_OFFER_ID, SECOND_TEST_USER_EMAIL);
        });
    }

    @Test
    @DisplayName("getOwnedOffers_whenUserHasOffers_thenReturnMappedOfferDtoPage")
    void getOwnedOffers_whenUserHasOffers_thenReturnMappedOfferDtoPage() {
        List<Offer> offers = getPopulatedOffers();
        List<OfferDTO> expected = getPopulatedOffersDTO();
        Pageable pageable = PageRequest.of(0, 20);

        when(offerRepository.findAllByOwnerEmail(TEST_USER_EMAIL, pageable))
                .thenReturn(new PageImpl<>(offers, pageable, offers.size()));

        Page<OfferDTO> actual = offerService.getOwnedOffers(TEST_USER_EMAIL, pageable);

        assertEquals(expected, actual.getContent());
        assertEquals(2, actual.getTotalElements());
    }

    @Test
    @DisplayName("getOwnedOffers_whenUserHasNoOffers_thenReturnEmptyPage")
    void getOwnedOffers_whenUserHasNoOffers_thenReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 20);

        when(offerRepository.findAllByOwnerEmail(TEST_USER_EMAIL, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<OfferDTO> actual = offerService.getOwnedOffers(TEST_USER_EMAIL, pageable);

        assertEquals(0, actual.getTotalElements());
        assertTrue(actual.getContent().isEmpty());
    }

    @Test
    @DisplayName("searchOffers_whenTermMatchesHotelCityCountry_thenReturnMatchingPagedOffers")
    void searchOffers_whenTermMatchesHotelCityCountry_thenReturnMatchingPagedOffers() {
        List<Offer> twoMatches = OfferAssembler.getPopulatedOffers();
        Pageable pageable = PageRequest.of(0, 20);

        when(offerRepository.searchByTerm(eq("testHotelName"), eq(pageable)))
                .thenReturn(new PageImpl<>(twoMatches, pageable, twoMatches.size()));

        Page<OfferDTO> actual = offerService.searchOffers("testHotelName", pageable);

        assertEquals(2, actual.getTotalElements());
    }

    @Test
    @DisplayName("searchOffers_whenTermMatchesOwnerEmailOnly_thenReturnEmptyPage")
    void searchOffers_whenTermMatchesOwnerEmailOnly_thenReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 20);

        when(offerRepository.searchByTerm(eq(TEST_OWNER_EMAIL), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<OfferDTO> actual = offerService.searchOffers(TEST_OWNER_EMAIL, pageable);

        assertEquals(0, actual.getTotalElements());
        assertTrue(actual.getContent().isEmpty());
    }

    @Test
    @DisplayName("searchOffers_whenPageSizeIsSmall_thenReturnOnlyPageSizeResults")
    void searchOffers_whenPageSizeIsSmall_thenReturnOnlyPageSizeResults() {
        Pageable pageable = PageRequest.of(0, 1);
        List<Offer> oneResult = List.of(OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID));

        when(offerRepository.searchByTerm(eq("testHotelName"), eq(pageable)))
                .thenReturn(new PageImpl<>(oneResult, pageable, 2));

        Page<OfferDTO> actual = offerService.searchOffers("testHotelName", pageable);

        assertEquals(1, actual.getContent().size());
        assertEquals(2, actual.getTotalElements());
    }

    @Test
    @DisplayName("editOffer_whenOfferExists_thenSaveAndReturnUpdatedOfferDto")
    void editOffer_whenOfferExists_thenSaveAndReturnUpdatedOfferDto() {
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        OfferEditDTO input = OfferAssembler.getPopulatedOfferEditDTO(TEST_DEFAULT_OFFER_ID);
        OfferDTO expected = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        ArgumentCaptor<Offer> eventCaptor = ArgumentCaptor.forClass(Offer.class);

        when(offerRepository.findById(offer.getId())).thenReturn(Optional.of(offer));
        when(offerRepository.save(any())).thenReturn(offer);
        doNothing().when(eventSourceService).saveEvent(eventCaptor.capture(), any());

        OfferDTO actual = offerService.editOffer(input);

        assertEquals(expected, actual);
        verify(eventSourceService).saveEvent(any(Offer.class), eq(EventType.OFFER_UPDATED));
        assertEquals(offer.getId(), eventCaptor.getValue().getId());
        assertEquals(offer.getOwnerEmail(), eventCaptor.getValue().getOwnerEmail());
    }

    @Test
    @DisplayName("editOffer_whenOfferIdIsNull_thenThrowOfferException")
    void editOffer_whenOfferIdIsNull_thenThrowOfferException() {
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        offer.setId(null);

        OfferEditDTO expected = OfferAssembler.getPopulatedOfferEditDTO(TEST_DEFAULT_OFFER_ID);
        expected.setId(null);

        when(offerRepository.findById(offer.getId())).thenReturn(Optional.empty());

        assertThrows(OfferException.class, () -> {
            offerService.editOffer(expected);
        });
    }

    @Test
    @DisplayName("findOfferOwner_whenOfferExists_thenReturnOwnerEmail")
    void findOfferOwner_whenOfferExists_thenReturnOwnerEmail() {
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);

        when(offerRepository.findById(offer.getId())).thenReturn(Optional.of(offer));

        String actual = offerService.findOfferOwner(TEST_DEFAULT_OFFER_ID);

        assertEquals(TEST_OWNER_EMAIL, actual);
    }

    @Test
    @DisplayName("findOfferOwner_whenOfferDoesNotExist_thenThrowOfferException")
    void findOfferOwner_whenOfferDoesNotExist_thenThrowOfferException() {
        when(offerRepository.findById(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.empty());

        assertThrows(OfferException.class, () -> {
            offerService.findOfferOwner(TEST_DEFAULT_OFFER_ID);
        });
    }

    @Test
    @DisplayName("uploadPhoto_whenCallerIsOwner_thenSetPhotoPathAndReturnUpdatedDto")
    void uploadPhoto_whenCallerIsOwner_thenSetPhotoPathAndReturnUpdatedDto() {
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        String expectedKey = "offers/test-uuid-hotel.jpg";
        String expectedUrl = "http://localhost:9000/sky-offers-test/" + expectedKey;

        when(offerRepository.findById(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.of(offer));
        when(offerRepository.save(any())).thenReturn(offer);
        when(photoStorage.upload(any(InputStream.class), anyLong(), eq("image/jpeg"), eq("hotel.jpg")))
                .thenReturn(expectedKey);
        when(photoStorage.presignedUrl(expectedKey)).thenReturn(expectedUrl);

        InputStream stream = new ByteArrayInputStream("bytes".getBytes());

        OfferDTO actual = offerService.uploadPhoto(
                TEST_DEFAULT_OFFER_ID, TEST_OWNER_EMAIL, stream, 5L, "image/jpeg", "hotel.jpg"
        );

        assertEquals(expectedKey, actual.getPhotoPath());
        assertEquals(expectedUrl, actual.getPhotoUrl());
        verify(photoStorage).upload(any(InputStream.class), anyLong(), eq("image/jpeg"), eq("hotel.jpg"));
        verify(offerRepository).save(any(Offer.class));
    }

    @Test
    @DisplayName("uploadPhoto_whenOfferDoesNotExist_thenThrowOfferException")
    void uploadPhoto_whenOfferDoesNotExist_thenThrowOfferException() {
        when(offerRepository.findById(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.empty());
        InputStream stream = new ByteArrayInputStream(new byte[0]);

        assertThrows(OfferException.class, () ->
                offerService.uploadPhoto(TEST_DEFAULT_OFFER_ID, TEST_OWNER_EMAIL, stream, 0L, "image/jpeg", "f.jpg")
        );
    }

    @Test
    @DisplayName("uploadPhoto_whenCallerIsNotOwner_thenThrowOfferException")
    void uploadPhoto_whenCallerIsNotOwner_thenThrowOfferException() {
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        when(offerRepository.findById(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.of(offer));
        InputStream stream = new ByteArrayInputStream(new byte[0]);

        assertThrows(OfferException.class, () ->
                offerService.uploadPhoto(TEST_DEFAULT_OFFER_ID, SECOND_TEST_USER_EMAIL, stream, 0L, "image/jpeg", "f.jpg")
        );
    }
}
