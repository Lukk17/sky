package com.lukk.sky.offer.domain.service;

import com.lukk.sky.offer.assemblers.OfferAssembler;
import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.adapters.dto.OfferEditDTO;
import com.lukk.sky.offer.domain.exception.OfferAccessDeniedException;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.exception.OfferNotFoundException;
import com.lukk.sky.offer.domain.exception.PhotoStorageUnavailableException;
import com.lukk.sky.offer.domain.model.EventType;
import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.ports.outbound.OfferNotificationService;
import com.lukk.sky.offer.domain.ports.outbound.OfferRepository;
import com.lukk.sky.offer.domain.ports.outbound.OfferSearch;
import com.lukk.sky.offer.domain.ports.outbound.PhotoStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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

import static com.lukk.sky.offer.assemblers.OfferAssembler.TEST_DEFAULT_OFFER_ID;
import static com.lukk.sky.offer.assemblers.OfferAssembler.TEST_DEFAULT_OFFER_ID_2;
import static com.lukk.sky.offer.assemblers.OfferAssembler.TEST_EXTERNAL_PHOTO_URL;
import static com.lukk.sky.offer.assemblers.OfferAssembler.testPhotoObjectKey;
import static com.lukk.sky.offer.assemblers.OfferAssembler.getPopulatedOffers;
import static com.lukk.sky.offer.assemblers.OfferAssembler.getPopulatedOffersDTO;
import static com.lukk.sky.offer.assemblers.UserAssembler.SECOND_TEST_USER_EMAIL;
import static com.lukk.sky.offer.assemblers.UserAssembler.TEST_OWNER_EMAIL;
import static com.lukk.sky.offer.assemblers.UserAssembler.TEST_USER_EMAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OfferServicePrimary: unit tests for the core offer service business logic")
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class OfferServicePrimaryTest {

    @Mock
    OfferRepository offerRepository;

    @Mock
    OfferSearch offerSearch;

    @Mock
    EventSourceService eventSourceService;

    @Mock
    PhotoStorage photoStorage;

    @Mock
    OfferNotificationService offerNotificationService;

    @InjectMocks
    OfferServicePrimary offerService;

    @Test
    @DisplayName("getAllOffers_whenOffersExist_thenReturnMappedOfferDtoPage")
    void getAllOffers_whenOffersExist_thenReturnMappedOfferDtoPage() {
        // given
        List<Offer> offers = OfferAssembler.getPopulatedOffers();
        Pageable pageable = PageRequest.of(0, 20);
        when(offerRepository.findAll(pageable)).thenReturn(new PageImpl<>(offers, pageable, offers.size()));
        List<OfferDTO> expected = OfferAssembler.getPopulatedOffersDTO();

        // when
        Page<OfferDTO> actual = offerService.getAllOffers(pageable);

        // then
        assertEquals(expected, actual.getContent());
        assertEquals(2, actual.getTotalElements());
    }

    @Test
    @DisplayName("getAllOffers_whenNoOffersSaved_thenReturnEmptyPage")
    void getAllOffers_whenNoOffersSaved_thenReturnEmptyPage() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        when(offerRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // when
        Page<OfferDTO> actual = offerService.getAllOffers(pageable);

        // then
        assertEquals(0, actual.getTotalElements());
        assertTrue(actual.getContent().isEmpty());
    }

    @Test
    @DisplayName("addOffer_whenValidOfferDto_thenSaveAndReturnOfferDto")
    void addOffer_whenValidOfferDto_thenSaveAndReturnOfferDto() throws OfferException {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        OfferDTO expected = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        when(offerRepository.save(any())).thenReturn(offer);
        doNothing().when(eventSourceService).saveEvent(any(), any());

        // when
        OfferDTO actual = offerService.addOffer(expected);

        // then
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("addOffer_whenClientSuppliesAnId_thenPersistWithoutItSoTheDatabaseAssignsOne")
    void addOffer_whenClientSuppliesAnId_thenPersistWithoutIt() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        OfferDTO input = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        ArgumentCaptor<Offer> savedCaptor = ArgumentCaptor.forClass(Offer.class);
        when(offerRepository.save(savedCaptor.capture())).thenReturn(offer);
        doNothing().when(eventSourceService).saveEvent(any(), any());

        // when
        offerService.addOffer(input);

        // then
        assertNull(savedCaptor.getValue().getId(),
                "a create must hand the entity to the repository without a client-chosen identifier");
    }

    @Test
    @DisplayName("deleteOffer_whenOfferExistsAndOwnerMatches_thenDeleteFromRepository")
    void deleteOffer_whenOfferExistsAndOwnerMatches_thenDeleteFromRepository() {
        // given
        Offer expected = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        ArgumentCaptor<Offer> valueCapture = ArgumentCaptor.forClass(Offer.class);
        doReturn(Optional.of(expected)).when(offerRepository).findById(expected.getId());
        doNothing().when(offerRepository).delete(valueCapture.capture());
        doNothing().when(eventSourceService).saveEvent(any(), any());

        // when
        offerService.deleteOffer(TEST_DEFAULT_OFFER_ID, TEST_OWNER_EMAIL);

        // then
        assertEquals(expected, valueCapture.getValue());
    }

    @Test
    @DisplayName("deleteOffer_whenOfferDoesNotExist_thenThrowOfferException")
    void deleteOffer_whenOfferDoesNotExist_thenThrowOfferException() {
        // given
        Offer expected = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        doReturn(Optional.empty()).when(offerRepository).findById(expected.getId());

        // when / then
        assertThrows(OfferException.class, () -> {
            offerService.deleteOffer(TEST_DEFAULT_OFFER_ID, TEST_USER_EMAIL);
        });
    }

    @Test
    @DisplayName("deleteOffer_whenRequesterIsNotOwner_thenThrowOfferAccessDeniedException")
    void deleteOffer_whenRequesterIsNotOwner_thenThrowOfferException() {
        // given
        Offer expected = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        doReturn(Optional.of(expected)).when(offerRepository).findById(expected.getId());

        // when / then
        assertThrows(OfferAccessDeniedException.class, () -> {
            offerService.deleteOffer(TEST_DEFAULT_OFFER_ID, SECOND_TEST_USER_EMAIL);
        });
    }

    @Test
    @DisplayName("getOwnedOffers_whenUserHasOffers_thenReturnMappedOfferDtoPage")
    void getOwnedOffers_whenUserHasOffers_thenReturnMappedOfferDtoPage() {
        // given
        List<Offer> offers = getPopulatedOffers();
        List<OfferDTO> expected = getPopulatedOffersDTO();
        Pageable pageable = PageRequest.of(0, 20);
        when(offerRepository.findAllByOwnerEmail(TEST_USER_EMAIL, pageable))
                .thenReturn(new PageImpl<>(offers, pageable, offers.size()));

        // when
        Page<OfferDTO> actual = offerService.getOwnedOffers(TEST_USER_EMAIL, pageable);

        // then
        assertEquals(expected, actual.getContent());
        assertEquals(2, actual.getTotalElements());
    }

    @Test
    @DisplayName("getOwnedOffers_whenUserHasNoOffers_thenReturnEmptyPage")
    void getOwnedOffers_whenUserHasNoOffers_thenReturnEmptyPage() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        when(offerRepository.findAllByOwnerEmail(TEST_USER_EMAIL, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // when
        Page<OfferDTO> actual = offerService.getOwnedOffers(TEST_USER_EMAIL, pageable);

        // then
        assertEquals(0, actual.getTotalElements());
        assertTrue(actual.getContent().isEmpty());
    }

    @Test
    @DisplayName("searchOffers_whenTermMatchesHotelCityCountry_thenReturnMatchingPagedOffers")
    void searchOffers_whenTermMatchesHotelCityCountry_thenReturnMatchingPagedOffers() {
        // given
        List<Offer> twoMatches = OfferAssembler.getPopulatedOffers();
        Pageable pageable = PageRequest.of(0, 20);
        when(offerSearch.searchByTerm(eq("testHotelName"), eq(pageable)))
                .thenReturn(new PageImpl<>(twoMatches, pageable, twoMatches.size()));

        // when
        Page<OfferDTO> actual = offerService.searchOffers("testHotelName", pageable);

        // then
        assertEquals(2, actual.getTotalElements());
    }

    @Test
    @DisplayName("searchOffers_whenTermMatchesOwnerEmailOnly_thenReturnEmptyPage")
    void searchOffers_whenTermMatchesOwnerEmailOnly_thenReturnEmptyPage() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        when(offerSearch.searchByTerm(eq(TEST_OWNER_EMAIL), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // when
        Page<OfferDTO> actual = offerService.searchOffers(TEST_OWNER_EMAIL, pageable);

        // then
        assertEquals(0, actual.getTotalElements());
        assertTrue(actual.getContent().isEmpty());
    }

    @Test
    @DisplayName("searchOffers_whenPageSizeIsSmall_thenReturnOnlyPageSizeResults")
    void searchOffers_whenPageSizeIsSmall_thenReturnOnlyPageSizeResults() {
        // given
        Pageable pageable = PageRequest.of(0, 1);
        List<Offer> oneResult = List.of(OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID));
        when(offerSearch.searchByTerm(eq("testHotelName"), eq(pageable)))
                .thenReturn(new PageImpl<>(oneResult, pageable, 2));

        // when
        Page<OfferDTO> actual = offerService.searchOffers("testHotelName", pageable);

        // then
        assertEquals(1, actual.getContent().size());
        assertEquals(2, actual.getTotalElements());
    }

    @Test
    @DisplayName("editOffer_whenOfferExists_thenSaveAndReturnUpdatedOfferDto")
    void editOffer_whenOfferExists_thenSaveAndReturnUpdatedOfferDto() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        OfferEditDTO input = OfferAssembler.getPopulatedOfferEditDTO(TEST_DEFAULT_OFFER_ID);
        OfferDTO expected = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        ArgumentCaptor<Offer> eventCaptor = ArgumentCaptor.forClass(Offer.class);
        when(offerRepository.findById(offer.getId())).thenReturn(Optional.of(offer));
        when(offerRepository.save(any())).thenReturn(offer);
        doNothing().when(eventSourceService).saveEvent(eventCaptor.capture(), any());

        // when
        OfferDTO actual = offerService.editOffer(input, TEST_OWNER_EMAIL);

        // then
        assertEquals(expected, actual);
        verify(eventSourceService).saveEvent(any(Offer.class), eq(EventType.OFFER_UPDATED));
        assertEquals(offer.getId(), eventCaptor.getValue().getId());
        assertEquals(offer.getOwnerEmail(), eventCaptor.getValue().getOwnerEmail());
    }

    @Test
    @DisplayName("editOffer_whenOfferIdIsNull_thenThrowOfferNotFoundException")
    void editOffer_whenOfferIdIsNull_thenThrowOfferNotFoundException() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        offer.setId(null);
        OfferEditDTO expected = OfferAssembler.getPopulatedOfferEditDTO(TEST_DEFAULT_OFFER_ID);
        expected.setId(null);
        when(offerRepository.findById(offer.getId())).thenReturn(Optional.empty());

        // when / then
        assertThrows(OfferNotFoundException.class, () -> {
            offerService.editOffer(expected, TEST_OWNER_EMAIL);
        });
    }

    @Test
    @DisplayName("editOffer_whenCallerIsNotOwner_thenThrowOfferAccessDeniedExceptionAndSaveNothing")
    void editOffer_whenCallerIsNotOwner_thenThrowOfferAccessDeniedException() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        OfferEditDTO input = OfferAssembler.getPopulatedOfferEditDTO(TEST_DEFAULT_OFFER_ID);
        when(offerRepository.findById(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.of(offer));

        // when / then
        assertThrows(OfferAccessDeniedException.class,
                () -> offerService.editOffer(input, SECOND_TEST_USER_EMAIL));

        verify(offerRepository, never()).save(any());
    }

    @Test
    @DisplayName("editOffer_whenOwnerEditsOffer_thenTheStoredOwnerIsKeptRatherThanTakenFromThePayload")
    void editOffer_whenOwnerEditsOffer_thenKeepTheStoredOwner() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        OfferEditDTO input = OfferAssembler.getPopulatedOfferEditDTO(TEST_DEFAULT_OFFER_ID);
        input.setOwnerEmail(SECOND_TEST_USER_EMAIL);
        ArgumentCaptor<Offer> savedCaptor = ArgumentCaptor.forClass(Offer.class);
        when(offerRepository.findById(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.of(offer));
        when(offerRepository.save(savedCaptor.capture())).thenReturn(offer);
        doNothing().when(eventSourceService).saveEvent(any(), any());

        // when
        offerService.editOffer(input, TEST_OWNER_EMAIL);

        // then
        assertEquals(TEST_OWNER_EMAIL, savedCaptor.getValue().getOwnerEmail(),
                "an edit must never move an offer to another owner");
    }

    @Test
    @DisplayName("findOfferOwner_whenOfferExists_thenReturnOwnerEmail")
    void findOfferOwner_whenOfferExists_thenReturnOwnerEmail() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        when(offerRepository.findById(offer.getId())).thenReturn(Optional.of(offer));

        // when
        String actual = offerService.findOfferOwner(TEST_DEFAULT_OFFER_ID);

        // then
        assertEquals(TEST_OWNER_EMAIL, actual);
    }

    @Test
    @DisplayName("findOfferOwner_whenOfferDoesNotExist_thenThrowOfferException")
    void findOfferOwner_whenOfferDoesNotExist_thenThrowOfferException() {
        // given
        when(offerRepository.findById(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.empty());

        // when / then
        assertThrows(OfferException.class, () -> {
            offerService.findOfferOwner(TEST_DEFAULT_OFFER_ID);
        });
    }

    @Test
    @DisplayName("uploadPhoto_whenCallerIsOwner_thenSetStoredKeyAndReturnUpdatedDto")
    void uploadPhoto_whenCallerIsOwner_thenSetStoredKeyAndReturnUpdatedDto() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        String expectedKey = testPhotoObjectKey(TEST_DEFAULT_OFFER_ID);
        String expectedUrl = "http://localhost:9000/sky-offers-test/" + expectedKey;
        when(offerRepository.findById(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.of(offer));
        when(offerRepository.save(any())).thenReturn(offer);
        when(photoStorage.upload(eq(TEST_DEFAULT_OFFER_ID), any(InputStream.class), anyLong(), eq("image/jpeg"), eq("hotel.jpg")))
                .thenReturn(expectedKey);
        when(photoStorage.presignedUrl(expectedKey)).thenReturn(expectedUrl);
        InputStream stream = new ByteArrayInputStream("bytes".getBytes());

        // when
        OfferDTO actual = offerService.uploadPhoto(
                TEST_DEFAULT_OFFER_ID, TEST_OWNER_EMAIL, stream, 5L, "image/jpeg", "hotel.jpg"
        );

        // then
        assertEquals(expectedKey, offer.getPhotoObjectKey());
        assertEquals(expectedUrl, actual.getPhotoUrl());
        verify(photoStorage).upload(eq(TEST_DEFAULT_OFFER_ID), any(InputStream.class), anyLong(), eq("image/jpeg"), eq("hotel.jpg"));
        verify(offerRepository).save(any(Offer.class));
    }

    @Test
    @DisplayName("uploadPhoto_whenOfferDoesNotExist_thenThrowOfferException")
    void uploadPhoto_whenOfferDoesNotExist_thenThrowOfferException() {
        // given
        when(offerRepository.findById(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.empty());
        InputStream stream = new ByteArrayInputStream(new byte[0]);

        // when / then
        assertThrows(OfferException.class, () ->
                offerService.uploadPhoto(TEST_DEFAULT_OFFER_ID, TEST_OWNER_EMAIL, stream, 0L, "image/jpeg", "f.jpg")
        );
    }

    @Test
    @DisplayName("uploadPhoto_whenCallerIsNotOwner_thenThrowOfferAccessDeniedException")
    void uploadPhoto_whenCallerIsNotOwner_thenThrowOfferException() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        when(offerRepository.findById(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.of(offer));
        InputStream stream = new ByteArrayInputStream(new byte[0]);

        // when / then
        assertThrows(OfferAccessDeniedException.class, () ->
                offerService.uploadPhoto(TEST_DEFAULT_OFFER_ID, SECOND_TEST_USER_EMAIL, stream, 0L, "image/jpeg", "f.jpg")
        );
    }

    @Test
    @DisplayName("deleteOffer_whenOfferHasPhoto_thenRemovesStoredObjectSoTheBucketKeepsNoOrphan")
    void deleteOffer_whenOfferHasPhoto_thenRemovesStoredObjectSoTheBucketKeepsNoOrphan() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        doReturn(Optional.of(offer)).when(offerRepository).findById(TEST_DEFAULT_OFFER_ID);

        // when
        offerService.deleteOffer(TEST_DEFAULT_OFFER_ID, TEST_OWNER_EMAIL);

        // then
        verify(photoStorage).delete(TEST_DEFAULT_OFFER_ID, testPhotoObjectKey(TEST_DEFAULT_OFFER_ID));
    }

    @Test
    @DisplayName("deleteOffer_whenOfferHasNoPhoto_thenLeavesStorageUntouched")
    void deleteOffer_whenOfferHasNoPhoto_thenLeavesStorageUntouched() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        offer.setPhotoObjectKey(null);
        doReturn(Optional.of(offer)).when(offerRepository).findById(TEST_DEFAULT_OFFER_ID);

        // when
        offerService.deleteOffer(TEST_DEFAULT_OFFER_ID, TEST_OWNER_EMAIL);

        // then
        verify(photoStorage, never()).delete(any(), any());
    }

    @Test
    @DisplayName("deleteOffer_whenStoredKeyIsBlank_thenLeavesStorageUntouched")
    void deleteOffer_whenStoredKeyIsBlank_thenLeavesStorageUntouched() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        offer.setPhotoObjectKey("   ");
        doReturn(Optional.of(offer)).when(offerRepository).findById(TEST_DEFAULT_OFFER_ID);

        // when
        offerService.deleteOffer(TEST_DEFAULT_OFFER_ID, TEST_OWNER_EMAIL);

        // then
        verify(photoStorage, never()).delete(any(), any());
    }

    @Test
    @DisplayName("deleteOffer_whenStorageDeleteFails_thenRowIsStillDeletedAndNoExceptionReachesTheCaller")
    void deleteOffer_whenStorageDeleteFails_thenRowIsStillDeletedAndNoExceptionReachesTheCaller() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        doReturn(Optional.of(offer)).when(offerRepository).findById(TEST_DEFAULT_OFFER_ID);
        doThrow(new PhotoStorageUnavailableException(
                "Photo delete failed. The object store is unavailable.", new IllegalStateException("refused")))
                .when(photoStorage).delete(TEST_DEFAULT_OFFER_ID, testPhotoObjectKey(TEST_DEFAULT_OFFER_ID));

        // when
        offerService.deleteOffer(TEST_DEFAULT_OFFER_ID, TEST_OWNER_EMAIL);

        // then
        verify(offerRepository).delete(offer);
        verify(eventSourceService).saveEvent(offer, EventType.OFFER_DELETED);
    }

    @Test
    @DisplayName("uploadPhoto_whenOfferAlreadyHasPhoto_thenRemovesThePreviousStoredObject")
    void uploadPhoto_whenOfferAlreadyHasPhoto_thenRemovesThePreviousStoredObject() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        String newKey = "offers/" + TEST_DEFAULT_OFFER_ID + "/new-uuid-hotel.jpg";
        when(offerRepository.findById(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.of(offer));
        when(offerRepository.save(any())).thenReturn(offer);
        when(photoStorage.upload(eq(TEST_DEFAULT_OFFER_ID), any(InputStream.class), anyLong(), eq("image/jpeg"), eq("hotel.jpg")))
                .thenReturn(newKey);
        InputStream stream = new ByteArrayInputStream("bytes".getBytes());

        // when
        offerService.uploadPhoto(TEST_DEFAULT_OFFER_ID, TEST_OWNER_EMAIL, stream, 5L, "image/jpeg", "hotel.jpg");

        // then
        verify(photoStorage).delete(TEST_DEFAULT_OFFER_ID, testPhotoObjectKey(TEST_DEFAULT_OFFER_ID));
        verify(photoStorage, never()).delete(TEST_DEFAULT_OFFER_ID, newKey);
    }

    @Test
    @DisplayName("uploadPhoto_whenStoredKeyMatchesThePreviousOne_thenKeepsTheObjectItJustWrote")
    void uploadPhoto_whenStoredKeyMatchesThePreviousOne_thenKeepsTheObjectItJustWrote() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        when(offerRepository.findById(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.of(offer));
        when(offerRepository.save(any())).thenReturn(offer);
        when(photoStorage.upload(eq(TEST_DEFAULT_OFFER_ID), any(InputStream.class), anyLong(), eq("image/jpeg"), eq("hotel.jpg")))
                .thenReturn(testPhotoObjectKey(TEST_DEFAULT_OFFER_ID));
        InputStream stream = new ByteArrayInputStream("bytes".getBytes());

        // when
        offerService.uploadPhoto(TEST_DEFAULT_OFFER_ID, TEST_OWNER_EMAIL, stream, 5L, "image/jpeg", "hotel.jpg");

        // then
        verify(photoStorage, never()).delete(any(), any());
    }

    @Test
    @DisplayName("editOffer_whenPayloadCarriesAnotherOffersStorageKeyAsItsExternalUrl_thenTheStoredKeyIsUntouched")
    void editOffer_whenPayloadCarriesAStorageKeyAsItsExternalUrl_thenTheStoredKeyIsUntouched() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        String storedKey = offer.getPhotoObjectKey();
        OfferEditDTO input = OfferAssembler.getPopulatedOfferEditDTO(TEST_DEFAULT_OFFER_ID);
        input.setExternalPhotoUrl(testPhotoObjectKey(TEST_DEFAULT_OFFER_ID_2));
        ArgumentCaptor<Offer> savedCaptor = ArgumentCaptor.forClass(Offer.class);
        when(offerRepository.findById(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.of(offer));
        when(offerRepository.save(savedCaptor.capture())).thenReturn(offer);

        // when
        offerService.editOffer(input, TEST_OWNER_EMAIL);

        // then
        assertEquals(storedKey, savedCaptor.getValue().getPhotoObjectKey(),
                "an edit must never move the record of where the stored object lives");
        assertNotEquals(testPhotoObjectKey(TEST_DEFAULT_OFFER_ID_2), savedCaptor.getValue().getPhotoObjectKey());
        verify(photoStorage, never()).delete(any(), any());
    }

    @Test
    @DisplayName("editOffer_whenOfferHasAStoredPhoto_thenTheResponseAddressIsStillThePresignedOne")
    void editOffer_whenOfferHasAStoredPhoto_thenTheResponseAddressIsStillThePresignedOne() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        String storedKey = offer.getPhotoObjectKey();
        String presigned = "http://localhost:9000/sky-offers-test/" + storedKey + "?X-Amz-Algorithm=AWS4-HMAC-SHA256";
        OfferEditDTO input = OfferAssembler.getPopulatedOfferEditDTO(TEST_DEFAULT_OFFER_ID);
        when(offerRepository.findById(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.of(offer));
        when(offerRepository.save(any())).thenReturn(offer);
        when(photoStorage.presignedUrl(storedKey)).thenReturn(presigned);

        // when
        OfferDTO actual = offerService.editOffer(input, TEST_OWNER_EMAIL);

        // then
        assertEquals(presigned, actual.getPhotoUrl());
        assertEquals(TEST_EXTERNAL_PHOTO_URL, actual.getExternalPhotoUrl());
    }

    @Test
    @DisplayName("addOffer_whenPayloadCarriesAnExternalUrl_thenNoStorageKeyIsPersisted")
    void addOffer_whenPayloadCarriesAnExternalUrl_thenNoStorageKeyIsPersisted() {
        // given
        OfferDTO input = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        ArgumentCaptor<Offer> savedCaptor = ArgumentCaptor.forClass(Offer.class);
        when(offerRepository.save(savedCaptor.capture())).thenReturn(offer);

        // when
        offerService.addOffer(input);

        // then
        assertNull(savedCaptor.getValue().getPhotoObjectKey(),
                "only the upload path may write the object key");
        assertEquals(TEST_EXTERNAL_PHOTO_URL, savedCaptor.getValue().getExternalPhotoUrl());
    }

    @Test
    @DisplayName("getAllOffers_whenOfferHasNoStoredObject_thenPhotoUrlIsTheExternalAddress")
    void getAllOffers_whenOfferHasNoStoredObject_thenPhotoUrlIsTheExternalAddress() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        offer.setPhotoObjectKey(null);
        Pageable pageable = PageRequest.of(0, 20);
        when(offerRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(offer), pageable, 1));

        // when
        Page<OfferDTO> actual = offerService.getAllOffers(pageable);

        // then
        assertEquals(TEST_EXTERNAL_PHOTO_URL, actual.getContent().getFirst().getPhotoUrl());
        verify(photoStorage, never()).presignedUrl(any());
    }

    @Test
    @DisplayName("getAllOffers_whenOfferHasNeitherStoredObjectNorExternalAddress_thenPhotoUrlIsNull")
    void getAllOffers_whenOfferHasNoPhotoAtAll_thenPhotoUrlIsNull() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        offer.setPhotoObjectKey("   ");
        offer.setExternalPhotoUrl(null);
        Pageable pageable = PageRequest.of(0, 20);
        when(offerRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(offer), pageable, 1));

        // when
        Page<OfferDTO> actual = offerService.getAllOffers(pageable);

        // then
        assertNull(actual.getContent().getFirst().getPhotoUrl());
        verify(photoStorage, never()).presignedUrl(any());
    }

    @Test
    @DisplayName("deletePhoto_whenCallerIsOwner_thenRemovesTheStoredObjectAndClearsTheKey")
    void deletePhoto_whenCallerIsOwner_thenRemovesTheStoredObjectAndClearsTheKey() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        String storedKey = offer.getPhotoObjectKey();
        ArgumentCaptor<Offer> savedCaptor = ArgumentCaptor.forClass(Offer.class);
        when(offerRepository.findById(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.of(offer));
        when(offerRepository.save(savedCaptor.capture())).thenReturn(offer);

        // when
        offerService.deletePhoto(TEST_DEFAULT_OFFER_ID, TEST_OWNER_EMAIL);

        // then
        verify(photoStorage).delete(TEST_DEFAULT_OFFER_ID, storedKey);
        assertNull(savedCaptor.getValue().getPhotoObjectKey());
    }

    @Test
    @DisplayName("deletePhoto_whenOfferHasNoStoredObject_thenLeavesStorageUntouched")
    void deletePhoto_whenOfferHasNoStoredObject_thenLeavesStorageUntouched() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        offer.setPhotoObjectKey(null);
        when(offerRepository.findById(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.of(offer));
        when(offerRepository.save(any())).thenReturn(offer);

        // when
        offerService.deletePhoto(TEST_DEFAULT_OFFER_ID, TEST_OWNER_EMAIL);

        // then
        verify(photoStorage, never()).delete(any(), any());
    }

    @Test
    @DisplayName("deletePhoto_whenCallerIsNotOwner_thenThrowOfferAccessDeniedExceptionAndTouchNothing")
    void deletePhoto_whenCallerIsNotOwner_thenThrowOfferAccessDeniedException() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        when(offerRepository.findById(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.of(offer));

        // when / then
        assertThrows(OfferAccessDeniedException.class,
                () -> offerService.deletePhoto(TEST_DEFAULT_OFFER_ID, SECOND_TEST_USER_EMAIL));

        verify(offerRepository, never()).save(any());
        verify(photoStorage, never()).delete(any(), any());
    }

    @Test
    @DisplayName("deletePhoto_whenOfferDoesNotExist_thenThrowOfferNotFoundException")
    void deletePhoto_whenOfferDoesNotExist_thenThrowOfferNotFoundException() {
        // given
        when(offerRepository.findById(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.empty());

        // when / then
        assertThrows(OfferNotFoundException.class,
                () -> offerService.deletePhoto(TEST_DEFAULT_OFFER_ID, TEST_OWNER_EMAIL));
    }

    @Test
    @DisplayName("deletePhoto_whenStorageDeleteFails_thenTheKeyIsStillClearedAndNoExceptionReachesTheCaller")
    void deletePhoto_whenStorageDeleteFails_thenTheKeyIsStillCleared() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        String storedKey = offer.getPhotoObjectKey();
        ArgumentCaptor<Offer> savedCaptor = ArgumentCaptor.forClass(Offer.class);
        when(offerRepository.findById(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.of(offer));
        when(offerRepository.save(savedCaptor.capture())).thenReturn(offer);
        doThrow(new PhotoStorageUnavailableException(
                "Photo delete failed. The object store is unavailable.", new IllegalStateException("refused")))
                .when(photoStorage).delete(TEST_DEFAULT_OFFER_ID, storedKey);

        // when
        offerService.deletePhoto(TEST_DEFAULT_OFFER_ID, TEST_OWNER_EMAIL);

        // then
        assertNull(savedCaptor.getValue().getPhotoObjectKey());
    }

    @Test
    @DisplayName("uploadPhoto_whenTheStoreIsUnavailable_thenTheFailureReachesTheCallerAndTheRowIsNotTouched")
    void uploadPhoto_whenTheStoreIsUnavailable_thenTheFailureReachesTheCaller() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        when(offerRepository.findById(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.of(offer));
        doThrow(new PhotoStorageUnavailableException(
                "Photo upload failed. The object store is unavailable.", new IllegalStateException("refused")))
                .when(photoStorage).upload(eq(TEST_DEFAULT_OFFER_ID), any(InputStream.class), anyLong(),
                        eq("image/jpeg"), eq("hotel.jpg"));
        InputStream stream = new ByteArrayInputStream("bytes".getBytes());

        // when / then
        assertThrows(PhotoStorageUnavailableException.class, () ->
                offerService.uploadPhoto(TEST_DEFAULT_OFFER_ID, TEST_OWNER_EMAIL, stream, 5L, "image/jpeg", "hotel.jpg")
        );
        verify(offerRepository, never()).save(any(Offer.class));
    }

    @Test
    @DisplayName("getAllOffers_whenThePhotoAddressCannotBeSigned_thenTheFailureReachesTheCaller")
    void getAllOffers_whenThePhotoAddressCannotBeSigned_thenTheFailureReachesTheCaller() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        Pageable pageable = PageRequest.of(0, 20);
        when(offerRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(offer), pageable, 1));
        when(photoStorage.presignedUrl(offer.getPhotoObjectKey()))
                .thenThrow(new PhotoStorageUnavailableException(
                        "Photo address could not be signed. The object store is unavailable.",
                        new IllegalStateException("signer failed")));

        // when / then
        assertThrows(PhotoStorageUnavailableException.class, () -> offerService.getAllOffers(pageable));
    }

    @Test
    @DisplayName("addOffer publishes the created offer only after the event append, so a failed append publishes nothing")
    void addOffer_whenOfferIsPersisted_thenPublishCreatedAfterTheEventAppend() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        OfferDTO input = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        when(offerRepository.save(any())).thenReturn(offer);

        // when
        OfferDTO actual = offerService.addOffer(input);

        // then
        InOrder order = inOrder(offerRepository, eventSourceService, offerNotificationService);
        order.verify(offerRepository).save(any(Offer.class));
        order.verify(eventSourceService).saveEvent(any(Offer.class), eq(EventType.OFFER_CREATED));
        order.verify(offerNotificationService).publishCreated(actual, TEST_OWNER_EMAIL);
    }

    @Test
    @DisplayName("addOffer publishes nothing when the event append loses the sequence race")
    void addOffer_whenTheEventAppendConflicts_thenPublishNothing() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        OfferDTO input = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        when(offerRepository.save(any())).thenReturn(offer);
        doThrow(new IllegalStateException("sequence conflict"))
                .when(eventSourceService).saveEvent(any(), any());

        // when / then
        assertThrows(IllegalStateException.class, () -> offerService.addOffer(input));
        verifyNoInteractions(offerNotificationService);
    }

    @Test
    @DisplayName("editOffer publishes the edited offer only after the event append")
    void editOffer_whenOfferIsSaved_thenPublishEditedAfterTheEventAppend() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        OfferEditDTO input = OfferAssembler.getPopulatedOfferEditDTO(TEST_DEFAULT_OFFER_ID);
        when(offerRepository.findById(offer.getId())).thenReturn(Optional.of(offer));
        when(offerRepository.save(any())).thenReturn(offer);

        // when
        OfferDTO actual = offerService.editOffer(input, TEST_OWNER_EMAIL);

        // then
        InOrder order = inOrder(eventSourceService, offerNotificationService);
        order.verify(eventSourceService).saveEvent(any(Offer.class), eq(EventType.OFFER_UPDATED));
        order.verify(offerNotificationService).publishEdited(actual, TEST_OWNER_EMAIL);
    }

    @Test
    @DisplayName("editOffer publishes nothing when the caller does not own the offer")
    void editOffer_whenCallerIsNotOwner_thenPublishNothing() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        OfferEditDTO input = OfferAssembler.getPopulatedOfferEditDTO(TEST_DEFAULT_OFFER_ID);
        when(offerRepository.findById(offer.getId())).thenReturn(Optional.of(offer));

        // when / then
        assertThrows(OfferAccessDeniedException.class,
                () -> offerService.editOffer(input, SECOND_TEST_USER_EMAIL));
        verifyNoInteractions(offerNotificationService);
    }

    @Test
    @DisplayName("deleteOffer publishes the deletion naming the offer id, after the row and the object are gone")
    void deleteOffer_whenOfferIsRemoved_thenPublishDeletedLast() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        when(offerRepository.findById(offer.getId())).thenReturn(Optional.of(offer));

        // when
        offerService.deleteOffer(TEST_DEFAULT_OFFER_ID, TEST_OWNER_EMAIL);

        // then
        InOrder order = inOrder(offerRepository, eventSourceService, photoStorage, offerNotificationService);
        order.verify(offerRepository).delete(offer);
        order.verify(eventSourceService).saveEvent(offer, EventType.OFFER_DELETED);
        order.verify(photoStorage).delete(TEST_DEFAULT_OFFER_ID, testPhotoObjectKey(TEST_DEFAULT_OFFER_ID));
        order.verify(offerNotificationService).publishDeleted(TEST_DEFAULT_OFFER_ID, TEST_OWNER_EMAIL);
    }

    @Test
    @DisplayName("deleteOffer publishes nothing when the caller does not own the offer")
    void deleteOffer_whenCallerIsNotOwner_thenPublishNothing() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        when(offerRepository.findById(offer.getId())).thenReturn(Optional.of(offer));

        // when / then
        assertThrows(OfferAccessDeniedException.class,
                () -> offerService.deleteOffer(TEST_DEFAULT_OFFER_ID, SECOND_TEST_USER_EMAIL));
        verifyNoInteractions(offerNotificationService);
    }
}
