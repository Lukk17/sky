package com.lukk.sky.offer.domain.service;

import com.lukk.sky.offer.domain.exception.GalleryLimitExceededException;
import com.lukk.sky.offer.domain.ports.inbound.OfferView;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.exception.OfferNotFoundException;
import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.model.OfferPhoto;
import com.lukk.sky.offer.domain.ports.outbound.OfferPhotoRepository;
import com.lukk.sky.offer.domain.ports.outbound.OfferRepository;
import com.lukk.sky.offer.domain.ports.outbound.PhotoStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Offer gallery delete")
@ExtendWith(MockitoExtension.class)
class OfferGalleryDeleteServiceTest {

    @Mock
    OfferRepository offerRepository;

    @Mock
    OfferPhotoRepository offerPhotoRepository;

    @Mock
    PhotoStorage photoStorage;

    @InjectMocks
    OfferGalleryService offerService;

    private Offer offer;
    private UUID offerId;
    private static final String OWNER = "owner@sky.dev";

    @BeforeEach
    void setUp() {
        offerId = UUID.randomUUID();
        offer = Offer.builder().id(offerId).hotelName("H").city("C").country("K")
                .price(new java.math.BigDecimal("100")).ownerEmail(OWNER)
                .roomCapacity(2L).build();
        when(offerRepository.findById(offerId)).thenReturn(Optional.of(offer));
    }

    @Test
    @DisplayName("deleteGalleryPhoto_whenPhotoBelongsToAnotherOffer_thenNotFound")
    void deleteGalleryPhoto_whenPhotoBelongsToAnotherOffer_thenNotFound() {
        Offer other = Offer.builder().id(UUID.randomUUID()).hotelName("H").city("C").country("K")
                .price(new java.math.BigDecimal("100")).ownerEmail(OWNER).roomCapacity(2L).build();
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(other).position(0).objectKey("k0").build();
        when(offerPhotoRepository.findById(p0.getId())).thenReturn(Optional.of(p0));

        assertThrows(OfferNotFoundException.class,
                () -> offerService.deleteGalleryPhoto(offerId, p0.getId(), OWNER));
    }

    @Test
    @DisplayName("deleteGalleryPhoto_whenStoreDeleteFails_thenStillReturns")
    void deleteGalleryPhoto_whenStoreDeleteFails_thenStillReturns() {
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").build();
        when(offerPhotoRepository.findById(p0.getId())).thenReturn(Optional.of(p0));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(new ArrayList<>());
        org.mockito.Mockito.doThrow(new com.lukk.sky.offer.domain.exception.PhotoStorageUnavailableException("down", new java.io.IOException("refused")))
                .when(photoStorage).delete(offerId, "k0");

        OfferView dto = offerService.deleteGalleryPhoto(offerId, p0.getId(), OWNER);

        verify(offerPhotoRepository).delete(p0);
        assertTrue(dto.gallery().isEmpty());
    }

    @Test
    @DisplayName("deleteGalleryPhoto_whenKeyIsBlank_thenSkipsStoreDelete")
    void deleteGalleryPhoto_whenKeyIsBlank_thenSkipsStoreDelete() {
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0)
                .externalUrl("https://cdn.example/x.png").build();
        when(offerPhotoRepository.findById(p0.getId())).thenReturn(Optional.of(p0));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(new ArrayList<>());

        offerService.deleteGalleryPhoto(offerId, p0.getId(), OWNER);

        verify(offerPhotoRepository).delete(p0);
        org.mockito.Mockito.verify(photoStorage, org.mockito.Mockito.never())
                .delete(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("deleteGalleryPhoto_whenRenumberFindsGap_thenClosesIt")
    void deleteGalleryPhoto_whenRenumberFindsGap_thenClosesIt() {
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").build();
        when(offerPhotoRepository.findById(p0.getId())).thenReturn(Optional.of(p0));
        OfferPhoto survivor = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(5).objectKey("k5").build();
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId))
                .thenReturn(new ArrayList<>(List.of(survivor)));

        offerService.deleteGalleryPhoto(offerId, p0.getId(), OWNER);

        assertEquals(0, survivor.getPosition());
    }

    @Test
    @DisplayName("deleteGalleryPhoto_whenPhotoExists_thenRemovesRowAndObject")
    void deleteGalleryPhoto_whenPhotoExists_thenRemovesRowAndObject() {
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").build();
        when(offerPhotoRepository.findById(p0.getId())).thenReturn(Optional.of(p0));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(new ArrayList<>());

        OfferView dto = offerService.deleteGalleryPhoto(offerId, p0.getId(), OWNER);

        verify(offerPhotoRepository).delete(p0);
        verify(photoStorage).delete(offerId, "k0");
        assertTrue(dto.gallery().isEmpty());
    }

    @Test
    @DisplayName("deleteGalleryPhoto_whenCoverDeleted_thenFirstRemainingIsPromoted")
    void deleteGalleryPhoto_whenCoverDeleted_thenFirstRemainingIsPromoted() {
        OfferPhoto cover = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0)
                .objectKey("k0").main(true).build();
        OfferPhoto next = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(1)
                .objectKey("k1").main(false).build();
        when(offerPhotoRepository.findById(cover.getId())).thenReturn(Optional.of(cover));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId))
                .thenReturn(new ArrayList<>(List.of(cover, next)), new ArrayList<>(List.of(next)));
        when(photoStorage.presignedUrl(anyString())).thenAnswer(inv -> "u-" + inv.getArgument(0));

        OfferView dto = offerService.deleteGalleryPhoto(offerId, cover.getId(), OWNER);

        verify(offerPhotoRepository).delete(cover);
        verify(offerPhotoRepository, org.mockito.Mockito.times(2)).saveAllAndFlush(anyList());
        assertTrue(next.isMain());
        assertEquals(0, next.getPosition());
        assertEquals(next.getId(), dto.gallery().get(0).id());
        assertTrue(dto.gallery().get(0).main());
        assertEquals("u-k1", dto.coverPhotoUrl());
    }

    @Test
    @DisplayName("deleteGalleryPhoto_whenKeyIsNull_thenSkipsStoreDelete")
    void deleteGalleryPhoto_whenKeyIsNull_thenSkipsStoreDelete() {
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).build();
        when(offerPhotoRepository.findById(p0.getId())).thenReturn(Optional.of(p0));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId))
                .thenReturn(new ArrayList<>(List.of(p0)));

        offerService.deleteGalleryPhoto(offerId, p0.getId(), OWNER);

        verify(offerPhotoRepository).delete(p0);
        org.mockito.Mockito.verify(photoStorage, org.mockito.Mockito.never())
                .delete(any(), any());
    }
}
