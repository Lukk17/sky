package com.lukk.sky.offer.domain.service;

import com.lukk.sky.offer.assemblers.OfferAssembler;
import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.adapters.dto.OfferEditDTO;
import com.lukk.sky.offer.domain.exception.OfferAccessDeniedException;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.exception.OfferNotFoundException;
import com.lukk.sky.offer.domain.exception.PhotoStorageUnavailableException;
import com.lukk.sky.offer.domain.ports.inbound.OfferView;
import com.lukk.sky.offer.domain.model.EventType;
import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.model.OfferPhoto;
import com.lukk.sky.offer.domain.ports.outbound.OfferNotificationService;
import com.lukk.sky.offer.domain.ports.outbound.OfferPhotoRepository;
import com.lukk.sky.offer.domain.ports.outbound.OfferRepository;
import com.lukk.sky.offer.domain.ports.outbound.OfferSearch;
import com.lukk.sky.offer.domain.ports.outbound.PhotoStorage;
import org.junit.jupiter.api.BeforeEach;
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
import static com.lukk.sky.common.test.TestUsers.SECOND_TEST_USER_EMAIL;
import static com.lukk.sky.common.test.TestUsers.TEST_OWNER_EMAIL;
import static com.lukk.sky.common.test.TestUsers.TEST_USER_EMAIL;
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
import static org.mockito.Mockito.when;

@DisplayName("OfferServicePrimary: CRUD, search, edit and events")
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

    @Mock
    OfferPhotoRepository offerPhotoRepository;

    @Mock
    OfferGalleryService galleryService;

    @InjectMocks
    OfferServicePrimary offerService;

    @BeforeEach
    void stubEmptyGallery() {
        org.mockito.Mockito.lenient()
                .when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
        org.mockito.Mockito.lenient()
                .when(galleryService.deleteGalleryForOffer(
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
    }

    @Test
    @DisplayName("getAllOffers_whenOffersExist_thenReturnMappedOfferDtoPage")
    void getAllOffers_whenOffersExist_thenReturnMappedOfferDtoPage() {
        // given
        List<Offer> offers = OfferAssembler.getPopulatedOffers();
        Pageable pageable = PageRequest.of(0, 20);
        when(offerRepository.findAll(pageable)).thenReturn(new PageImpl<>(offers, pageable, offers.size()));
        List<OfferDTO> expected = OfferAssembler.getPopulatedOffersDTO();

        // when
        Page<OfferView> actual = offerService.getAllOffers(pageable);

        // then
        assertEquals(expected, actual.getContent().stream().map(OfferAssembler::toOfferDTO).toList());
        assertEquals(2, actual.getTotalElements());
    }
    @Test
    @DisplayName("getAllOffers_whenNoOffersSaved_thenReturnEmptyPage")
    void getAllOffers_whenNoOffersSaved_thenReturnEmptyPage() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        when(offerRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // when
        Page<OfferView> actual = offerService.getAllOffers(pageable);

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
        OfferView actual = offerService.addOffer(OfferAssembler.toCreateCommand(expected));

        // then
        assertEquals(expected, OfferAssembler.toOfferDTO(actual));
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
        offerService.addOffer(OfferAssembler.toCreateCommand(input));

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
        Page<OfferView> actual = offerService.getOwnedOffers(TEST_USER_EMAIL, pageable);

        // then
        assertEquals(expected, actual.getContent().stream().map(OfferAssembler::toOfferDTO).toList());
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
        Page<OfferView> actual = offerService.getOwnedOffers(TEST_USER_EMAIL, pageable);

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
        Page<OfferView> actual = offerService.searchOffers("testHotelName", pageable);

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
        Page<OfferView> actual = offerService.searchOffers(TEST_OWNER_EMAIL, pageable);

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
        Page<OfferView> actual = offerService.searchOffers("testHotelName", pageable);

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
        OfferView actual = offerService.editOffer(OfferAssembler.toEditCommand(input), TEST_OWNER_EMAIL);

        // then
        assertEquals(expected, OfferAssembler.toOfferDTO(actual));
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
            offerService.editOffer(OfferAssembler.toEditCommand(expected), TEST_OWNER_EMAIL);
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
                () -> offerService.editOffer(OfferAssembler.toEditCommand(input), SECOND_TEST_USER_EMAIL));

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
        offerService.editOffer(OfferAssembler.toEditCommand(input), TEST_OWNER_EMAIL);

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
    @DisplayName("addOffer publishes the created offer only after the event append, so a failed append publishes nothing")
    void addOffer_whenOfferIsPersisted_thenPublishCreatedAfterTheEventAppend() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        OfferDTO input = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        when(offerRepository.save(any())).thenReturn(offer);

        // when
        OfferView actual = offerService.addOffer(OfferAssembler.toCreateCommand(input));

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
        assertThrows(IllegalStateException.class, () -> offerService.addOffer(OfferAssembler.toCreateCommand(input)));
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
        OfferView actual = offerService.editOffer(OfferAssembler.toEditCommand(input), TEST_OWNER_EMAIL);

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
                () -> offerService.editOffer(OfferAssembler.toEditCommand(input), SECOND_TEST_USER_EMAIL));
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
