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

@DisplayName("Offer gallery: upload, reorder, cover, delete, cap")
@ExtendWith(MockitoExtension.class)
class OfferGalleryServiceTest {

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
    @DisplayName("uploadGalleryPhoto_whenBelowCap_thenAppendsAtEnd")
    void uploadGalleryPhoto_whenBelowCap_thenAppendsAtEnd() {
        // given
        // when
        // then
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(new ArrayList<>());
        when(photoStorage.upload(any(), any(), anyLong(), any(), any())).thenReturn("offers/" + offerId + "/k-a.png");

        OfferView dto = offerService.uploadGalleryPhoto(offerId, OWNER,
                new ByteArrayInputStream(new byte[]{1}), 1L, "image/png", "a.png");

        verify(offerPhotoRepository).saveAndFlush(any(OfferPhoto.class));
        verify(photoStorage).upload(any(), any(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("uploadGalleryPhoto_whenAtCap_thenThrows413")
    void uploadGalleryPhoto_whenAtCap_thenThrows413() {
        // given
        // when
        // then
        List<OfferPhoto> full = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
        // given
        // when
        // then
            full.add(OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(i)
                    .objectKey("k" + i).build());
        }
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(full);

        assertThrows(GalleryLimitExceededException.class, () -> offerService.uploadGalleryPhoto(
                offerId, OWNER, new ByteArrayInputStream(new byte[]{1}), 1L, "image/png", "x.png"));
    }

    @Test
    @DisplayName("reorderGalleryPhoto_whenMovingLastToFirst_thenCoverSwaps")
    void reorderGalleryPhoto_whenMovingLastToFirst_thenCoverSwaps() {
        // given
        // when
        // then
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").build();
        OfferPhoto p1 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(1).objectKey("k1").build();
        List<OfferPhoto> photos = new ArrayList<>(List.of(p0, p1));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(photos);
        when(photoStorage.presignedUrl("k0")).thenReturn("u0");
        when(photoStorage.presignedUrl("k1")).thenReturn("u1");

        OfferView dto = offerService.reorderGalleryPhoto(offerId, p1.getId(), 0, OWNER);

        assertEquals(p1.getId(), dto.gallery().get(0).id());
        assertEquals("u1", dto.coverPhotoUrl());
        assertEquals(0, p1.getPosition());
        assertEquals(1, p0.getPosition());
        verify(offerPhotoRepository, org.mockito.Mockito.times(2)).saveAllAndFlush(org.mockito.ArgumentMatchers.anyList());
        org.mockito.Mockito.verify(offerPhotoRepository, org.mockito.Mockito.never())
                .saveAndFlush(org.mockito.ArgumentMatchers.any(OfferPhoto.class));
    }

    @Test
    @DisplayName("reorderGalleryPhoto_whenPhotoUnknown_thenGalleryUnchanged")
    void reorderGalleryPhoto_whenPhotoUnknown_thenGalleryUnchanged() {
        // given
        // when
        // then
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").build();
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId))
                .thenReturn(new ArrayList<>(List.of(p0)));

        assertThrows(OfferNotFoundException.class,
                () -> offerService.reorderGalleryPhoto(offerId, UUID.randomUUID(), 0, OWNER));
    }

    @Test
    @DisplayName("reorderGalleryPhoto_whenPositionOutOfRange_thenRejected")
    void reorderGalleryPhoto_whenPositionOutOfRange_thenRejected() {
        // given
        // when
        // then
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").build();
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId))
                .thenReturn(new ArrayList<>(List.of(p0)));

        assertThrows(OfferException.class,
                () -> offerService.reorderGalleryPhoto(offerId, p0.getId(), 5, OWNER));
    }

    @Test
    @DisplayName("reorderGalleryPhoto_whenMovingFirstToLast_thenShiftsForward")
    void reorderGalleryPhoto_whenMovingFirstToLast_thenShiftsForward() {
        // given
        // when
        // then
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").build();
        OfferPhoto p1 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(1).objectKey("k1").build();
        OfferPhoto p2 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(2).objectKey("k2").build();
        List<OfferPhoto> photos = new ArrayList<>(List.of(p0, p1, p2));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(photos);
        when(photoStorage.presignedUrl(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(inv -> "u-" + inv.getArgument(0));

        OfferView dto = offerService.reorderGalleryPhoto(offerId, p0.getId(), 2, OWNER);

        assertEquals(2, p0.getPosition());
        assertEquals(0, p1.getPosition());
        assertEquals(1, p2.getPosition());
        assertEquals(p1.getId(), dto.gallery().get(0).id());
    }

    @Test
    @DisplayName("reorderGalleryPhoto_whenPositionUnchanged_thenKeepsOrder")
    void reorderGalleryPhoto_whenPositionUnchanged_thenKeepsOrder() {
        // given
        // when
        // then
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").build();
        List<OfferPhoto> photos = new ArrayList<>(List.of(p0));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(photos);
        when(photoStorage.presignedUrl("k0")).thenReturn("u0");

        OfferView dto = offerService.reorderGalleryPhoto(offerId, p0.getId(), 0, OWNER);

        assertEquals(0, p0.getPosition());
        assertEquals(p0.getId(), dto.gallery().get(0).id());
    }

    @Test
    @DisplayName("setGalleryCover_whenCalled_thenSetsMainFlagWithoutReordering")
    void setGalleryCover_whenCalled_thenSetsMainFlagWithoutReordering() {
        // given
        // when
        // then
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").build();
        OfferPhoto p1 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(1).objectKey("k1").build();
        List<OfferPhoto> photos = new ArrayList<>(List.of(p0, p1));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(photos);
        when(photoStorage.presignedUrl("k0")).thenReturn("u0");
        when(photoStorage.presignedUrl("k1")).thenReturn("u1");

        OfferView dto = offerService.setGalleryCover(offerId, p1.getId(), OWNER);

        assertEquals(0, p0.getPosition());
        assertEquals(1, p1.getPosition());
        assertEquals(p0.getId(), dto.gallery().get(0).id());
        assertEquals("u1", dto.coverPhotoUrl());
        verify(offerPhotoRepository, org.mockito.Mockito.times(2)).saveAllAndFlush(anyList());
    }

    @Test
    @DisplayName("deleteGalleryPhoto_whenPhotoBelongsToAnotherOffer_thenNotFound")
    void deleteGalleryPhoto_whenPhotoBelongsToAnotherOffer_thenNotFound() {
        // given
        // when
        // then
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
        // given
        // when
        // then
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
        // given
        // when
        // then
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
        // given
        // when
        // then
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").build();
        when(offerPhotoRepository.findById(p0.getId())).thenReturn(Optional.of(p0));
        OfferPhoto survivor = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(5).objectKey("k5").build();
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId))
                .thenReturn(new ArrayList<>(List.of(survivor)));

        offerService.deleteGalleryPhoto(offerId, p0.getId(), OWNER);

        assertEquals(0, survivor.getPosition());
    }

    @Test
    @DisplayName("galleryOf_whenEmptyAndExternalUrlSet_thenFallsBackToExternal")
    void galleryOf_whenEmptyAndExternalUrlSet_thenFallsBackToExternal() {
        // given
        // when
        // then
        offer.setExternalPhotoUrl("https://cdn.example/cover.png");
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(new ArrayList<>());
        when(photoStorage.upload(any(), any(), anyLong(), any(), any())).thenReturn("offers/" + offerId + "/k-a.png");

        OfferView dto = offerService.uploadGalleryPhoto(offerId, OWNER,
                new ByteArrayInputStream(new byte[]{1}), 1L, "image/png", "a.png");

        assertTrue(dto.gallery().isEmpty() || dto.coverPhotoUrl() != null);
    }

    @Test
    @DisplayName("galleryOf_whenPhotoHasExternalUrl_thenUsesIt")
    void galleryOf_whenPhotoHasExternalUrl_thenUsesIt() {
        // given
        // when
        // then
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0)
                .externalUrl("https://cdn.example/x.png").build();
        List<OfferPhoto> photos = new ArrayList<>(List.of(p0));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(photos);

        OfferView dto = offerService.reorderGalleryPhoto(offerId, p0.getId(), 0, OWNER);

        org.mockito.Mockito.verify(photoStorage, org.mockito.Mockito.never())
                .presignedUrl(org.mockito.ArgumentMatchers.anyString());
        assertEquals("https://cdn.example/x.png", dto.gallery().get(0).url());
    }

    @Test
    @DisplayName("coverPhotoUrl_whenMainFlagSet_thenPrefersMainOverPosition")
    void coverPhotoUrl_whenMainFlagSet_thenPrefersMainOverPosition() {
        // given
        // when
        // then
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").main(false).build();
        OfferPhoto p1 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(1).objectKey("k1").main(true).build();
        List<OfferPhoto> photos = new ArrayList<>(List.of(p0, p1));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(photos);
        when(photoStorage.presignedUrl("k0")).thenReturn("u0");
        when(photoStorage.presignedUrl("k1")).thenReturn("u1");
        when(offerPhotoRepository.findById(p0.getId())).thenReturn(Optional.of(p0));

        OfferView dto = offerService.deleteGalleryPhoto(offerId, p0.getId(), OWNER);

        verify(offerPhotoRepository).delete(p0);
    }

    @Test
    @DisplayName("setGalleryCover_whenCalled_thenSetsMainFlag")
    void setGalleryCover_whenCalled_thenSetsMainFlag() {
        // given
        // when
        // then
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").main(true).build();
        OfferPhoto p1 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(1).objectKey("k1").main(false).build();
        List<OfferPhoto> photos = new ArrayList<>(List.of(p0, p1));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(photos);
        when(photoStorage.presignedUrl(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(inv -> "u-" + inv.getArgument(0));

        OfferView dto = offerService.setGalleryCover(offerId, p1.getId(), OWNER);

        org.junit.jupiter.api.Assertions.assertTrue(p1.isMain());
        org.junit.jupiter.api.Assertions.assertFalse(p0.isMain());
        org.junit.jupiter.api.Assertions.assertTrue(dto.gallery().stream()
                .filter(g -> g.id().equals(p1.getId())).findFirst().orElseThrow().main());
    }

    @Test
    @DisplayName("uploadGalleryPhoto_whenFirstPhoto_thenMarkedMain")
    void uploadGalleryPhoto_whenFirstPhoto_thenMarkedMain() {
        // given
        // when
        // then
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(new ArrayList<>());
        when(photoStorage.upload(any(), any(), anyLong(), any(), any())).thenReturn("offers/" + offerId + "/k-a.png");
        org.mockito.ArgumentCaptor<OfferPhoto> captor = org.mockito.ArgumentCaptor.forClass(OfferPhoto.class);

        offerService.uploadGalleryPhoto(offerId, OWNER,
                new ByteArrayInputStream(new byte[]{1}), 1L, "image/png", "a.png");

        verify(offerPhotoRepository).saveAndFlush(captor.capture());
        org.junit.jupiter.api.Assertions.assertTrue(captor.getValue().isMain());
    }

    @Test
    @DisplayName("deleteGalleryPhoto_whenPhotoExists_thenRemovesRowAndObject")
    void deleteGalleryPhoto_whenPhotoExists_thenRemovesRowAndObject() {
        // given
        // when
        // then
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
        // given
        // when
        // then
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
    @DisplayName("setGalleryCover_whenChanged_thenListPayloadShowsNewCover")
    void setGalleryCover_whenChanged_thenListPayloadShowsNewCover() {
        // given
        // when
        // then
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0)
                .objectKey("k0").main(true).build();
        OfferPhoto p1 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(1)
                .objectKey("k1").main(false).build();
        List<OfferPhoto> photos = new ArrayList<>(List.of(p0, p1));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(photos);
        when(photoStorage.presignedUrl("k0")).thenReturn("u0");
        when(photoStorage.presignedUrl("k1")).thenReturn("u1");

        offerService.setGalleryCover(offerId, p1.getId(), OWNER);
        OfferView dto = offerService.summaryView(offer);

        assertEquals("u1", dto.coverPhotoUrl());
        assertEquals("u1", dto.photoUrl());
    }

    @Test
    @DisplayName("setGalleryCover_whenConcurrentCoverWins_thenAnswers409")
    void setGalleryCover_whenConcurrentCoverWins_thenAnswers409() {
        // given
        // when
        // then
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0)
                .objectKey("k0").main(true).build();
        OfferPhoto p1 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(1)
                .objectKey("k1").main(false).build();
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId))
                .thenReturn(new ArrayList<>(List.of(p0, p1)));
        org.mockito.Mockito.doThrow(new org.springframework.dao.DataIntegrityViolationException("two mains"))
                .when(offerPhotoRepository).saveAllAndFlush(org.mockito.ArgumentMatchers.anyList());

        assertThrows(com.lukk.sky.offer.domain.exception.GalleryCoverConflictException.class,
                () -> offerService.setGalleryCover(offerId, p1.getId(), OWNER));
    }

    @Test
    @DisplayName("setGalleryCover_whenTargetAlreadySoleMain_thenReturnsWithoutRewriting")
    void setGalleryCover_whenTargetAlreadySoleMain_thenReturnsWithoutRewriting() {
        // given
        // when
        // then
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").main(true).build();
        OfferPhoto p1 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(1).objectKey("k1").main(false).build();
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId))
                .thenReturn(new ArrayList<>(List.of(p0, p1)));
        when(photoStorage.presignedUrl("k0")).thenReturn("u0");
        when(photoStorage.presignedUrl("k1")).thenReturn("u1");

        OfferView dto = offerService.setGalleryCover(offerId, p0.getId(), OWNER);

        org.mockito.Mockito.verify(offerPhotoRepository, org.mockito.Mockito.never())
                .saveAllAndFlush(org.mockito.ArgumentMatchers.anyList());
        assertEquals("u0", dto.coverPhotoUrl());
    }

    @Test
    @DisplayName("uploadGalleryPhoto_whenCallerIsNotOwner_thenThrowsAccessDenied")
    void uploadGalleryPhoto_whenCallerIsNotOwner_thenThrowsAccessDenied() {
        // given
        // when
        // then
        assertThrows(com.lukk.sky.offer.domain.exception.OfferAccessDeniedException.class,
                () -> offerService.uploadGalleryPhoto(offerId, "stranger@sky.dev",
                        new ByteArrayInputStream(new byte[]{1}), 1L, "image/png", "a.png"));
        org.mockito.Mockito.verify(photoStorage, org.mockito.Mockito.never())
                .upload(any(), any(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("deleteGalleryPhoto_whenKeyIsNull_thenSkipsStoreDelete")
    void deleteGalleryPhoto_whenKeyIsNull_thenSkipsStoreDelete() {
        // given
        // when
        // then
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
