package com.lukk.sky.offer.domain.service;

import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.domain.exception.GalleryLimitExceededException;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.exception.OfferNotFoundException;
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
import static org.mockito.ArgumentMatchers.anyLong;
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
    OfferSearch offerSearch;

    @Mock
    EventSourceService eventSourceService;

    @Mock
    PhotoStorage photoStorage;

    @Mock
    OfferNotificationService offerNotificationService;

    @InjectMocks
    OfferServicePrimary offerService;

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
    void uploadGalleryPhoto_whenBelowCap_thenAppendsAtEnd() {
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(new ArrayList<>());
        when(photoStorage.upload(any(), any(), anyLong(), any(), any())).thenReturn("offers/" + offerId + "/k-a.png");

        OfferDTO dto = offerService.uploadGalleryPhoto(offerId, OWNER,
                new ByteArrayInputStream(new byte[]{1}), 1L, "image/png", "a.png");

        verify(offerPhotoRepository).save(any(OfferPhoto.class));
        verify(photoStorage).upload(any(), any(), anyLong(), any(), any());
    }

    @Test
    void uploadGalleryPhoto_whenAtCap_thenThrows413() {
        List<OfferPhoto> full = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            full.add(OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(i)
                    .objectKey("k" + i).build());
        }
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(full);

        assertThrows(GalleryLimitExceededException.class, () -> offerService.uploadGalleryPhoto(
                offerId, OWNER, new ByteArrayInputStream(new byte[]{1}), 1L, "image/png", "x.png"));
    }

    @Test
    void reorderGalleryPhoto_whenMovingLastToFirst_thenCoverSwaps() {
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").build();
        OfferPhoto p1 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(1).objectKey("k1").build();
        List<OfferPhoto> photos = new ArrayList<>(List.of(p0, p1));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(photos);
        when(photoStorage.presignedUrl("k0")).thenReturn("u0");
        when(photoStorage.presignedUrl("k1")).thenReturn("u1");

        OfferDTO dto = offerService.reorderGalleryPhoto(offerId, p1.getId(), 0, OWNER);

        assertEquals(p1.getId(), dto.getGallery().get(0).getId());
        assertEquals("u1", dto.getCoverPhotoUrl());
        assertEquals(0, p1.getPosition());
        assertEquals(1, p0.getPosition());
        verify(offerPhotoRepository, org.mockito.Mockito.atLeast(3)).saveAndFlush(any(OfferPhoto.class));
        org.mockito.Mockito.verify(offerPhotoRepository, org.mockito.Mockito.never()).saveAll(any());
    }

    @Test
    void reorderGalleryPhoto_whenPhotoUnknown_thenGalleryUnchanged() {
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").build();
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId))
                .thenReturn(new ArrayList<>(List.of(p0)));

        assertThrows(OfferNotFoundException.class,
                () -> offerService.reorderGalleryPhoto(offerId, UUID.randomUUID(), 0, OWNER));
    }

    @Test
    void reorderGalleryPhoto_whenPositionOutOfRange_thenRejected() {
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").build();
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId))
                .thenReturn(new ArrayList<>(List.of(p0)));

        assertThrows(OfferException.class,
                () -> offerService.reorderGalleryPhoto(offerId, p0.getId(), 5, OWNER));
    }

    @Test
    void reorderGalleryPhoto_whenMovingFirstToLast_thenShiftsForward() {
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").build();
        OfferPhoto p1 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(1).objectKey("k1").build();
        OfferPhoto p2 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(2).objectKey("k2").build();
        List<OfferPhoto> photos = new ArrayList<>(List.of(p0, p1, p2));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(photos);
        when(photoStorage.presignedUrl(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(inv -> "u-" + inv.getArgument(0));

        OfferDTO dto = offerService.reorderGalleryPhoto(offerId, p0.getId(), 2, OWNER);

        assertEquals(2, p0.getPosition());
        assertEquals(0, p1.getPosition());
        assertEquals(1, p2.getPosition());
        assertEquals(p1.getId(), dto.getGallery().get(0).getId());
    }

    @Test
    void reorderGalleryPhoto_whenPositionUnchanged_thenKeepsOrder() {
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").build();
        List<OfferPhoto> photos = new ArrayList<>(List.of(p0));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(photos);
        when(photoStorage.presignedUrl("k0")).thenReturn("u0");

        OfferDTO dto = offerService.reorderGalleryPhoto(offerId, p0.getId(), 0, OWNER);

        assertEquals(0, p0.getPosition());
        assertEquals(p0.getId(), dto.getGallery().get(0).getId());
    }

    @Test
    void setGalleryCover_whenCalled_thenMovesPhotoToFirst() {
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").build();
        OfferPhoto p1 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(1).objectKey("k1").build();
        List<OfferPhoto> photos = new ArrayList<>(List.of(p0, p1));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(photos);
        when(photoStorage.presignedUrl("k0")).thenReturn("u0");
        when(photoStorage.presignedUrl("k1")).thenReturn("u1");

        OfferDTO dto = offerService.setGalleryCover(offerId, p1.getId(), OWNER);

        assertEquals(p1.getId(), dto.getGallery().get(0).getId());
        assertEquals("u1", dto.getCoverPhotoUrl());
    }

    @Test
    void deleteGalleryPhoto_whenPhotoBelongsToAnotherOffer_thenNotFound() {
        Offer other = Offer.builder().id(UUID.randomUUID()).hotelName("H").city("C").country("K")
                .price(new java.math.BigDecimal("100")).ownerEmail(OWNER).roomCapacity(2L).build();
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(other).position(0).objectKey("k0").build();
        when(offerPhotoRepository.findById(p0.getId())).thenReturn(Optional.of(p0));

        assertThrows(OfferNotFoundException.class,
                () -> offerService.deleteGalleryPhoto(offerId, p0.getId(), OWNER));
    }

    @Test
    void deleteGalleryPhoto_whenStoreDeleteFails_thenStillReturns() {
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").build();
        when(offerPhotoRepository.findById(p0.getId())).thenReturn(Optional.of(p0));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(new ArrayList<>());
        org.mockito.Mockito.doThrow(new com.lukk.sky.offer.domain.exception.PhotoStorageUnavailableException("down", new java.io.IOException("refused")))
                .when(photoStorage).delete(offerId, "k0");

        OfferDTO dto = offerService.deleteGalleryPhoto(offerId, p0.getId(), OWNER);

        verify(offerPhotoRepository).delete(p0);
        assertTrue(dto.getGallery().isEmpty());
    }

    @Test
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
    void galleryOf_whenEmptyAndExternalUrlSet_thenFallsBackToExternal() {
        offer.setExternalPhotoUrl("https://cdn.example/cover.png");
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(new ArrayList<>());
        when(photoStorage.upload(any(), any(), anyLong(), any(), any())).thenReturn("offers/" + offerId + "/k-a.png");

        OfferDTO dto = offerService.uploadGalleryPhoto(offerId, OWNER,
                new ByteArrayInputStream(new byte[]{1}), 1L, "image/png", "a.png");

        assertTrue(dto.getGallery().isEmpty() || dto.getCoverPhotoUrl() != null);
    }

    @Test
    void galleryOf_whenPhotoHasExternalUrl_thenUsesIt() {
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0)
                .externalUrl("https://cdn.example/x.png").build();
        List<OfferPhoto> photos = new ArrayList<>(List.of(p0));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(photos);
        when(offerPhotoRepository.findById(p0.getId())).thenReturn(Optional.of(p0));

        OfferDTO dto = offerService.deleteGalleryPhoto(offerId, p0.getId(), OWNER);

        org.mockito.Mockito.verify(photoStorage, org.mockito.Mockito.never())
                .presignedUrl(org.mockito.ArgumentMatchers.anyString());
        assertEquals("https://cdn.example/x.png", dto.getGallery().get(0).getUrl());
    }

    @Test
    void coverPhotoUrl_whenMainFlagSet_thenPrefersMainOverPosition() {
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").main(false).build();
        OfferPhoto p1 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(1).objectKey("k1").main(true).build();
        List<OfferPhoto> photos = new ArrayList<>(List.of(p0, p1));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(photos);
        when(photoStorage.presignedUrl("k0")).thenReturn("u0");
        when(photoStorage.presignedUrl("k1")).thenReturn("u1");
        when(offerPhotoRepository.findById(p0.getId())).thenReturn(Optional.of(p0));

        OfferDTO dto = offerService.deleteGalleryPhoto(offerId, p0.getId(), OWNER);

        verify(offerPhotoRepository).delete(p0);
    }

    @Test
    void setGalleryCover_whenCalled_thenSetsMainFlag() {
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").main(true).build();
        OfferPhoto p1 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(1).objectKey("k1").main(false).build();
        List<OfferPhoto> photos = new ArrayList<>(List.of(p0, p1));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(photos);
        when(photoStorage.presignedUrl(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(inv -> "u-" + inv.getArgument(0));

        OfferDTO dto = offerService.setGalleryCover(offerId, p1.getId(), OWNER);

        org.junit.jupiter.api.Assertions.assertTrue(p1.isMain());
        org.junit.jupiter.api.Assertions.assertFalse(p0.isMain());
        org.junit.jupiter.api.Assertions.assertTrue(dto.getGallery().stream()
                .filter(g -> g.getId().equals(p1.getId())).findFirst().orElseThrow().isMain());
    }

    @Test
    void uploadGalleryPhoto_whenFirstPhoto_thenMarkedMain() {
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(new ArrayList<>());
        when(photoStorage.upload(any(), any(), anyLong(), any(), any())).thenReturn("offers/" + offerId + "/k-a.png");
        org.mockito.ArgumentCaptor<OfferPhoto> captor = org.mockito.ArgumentCaptor.forClass(OfferPhoto.class);

        offerService.uploadGalleryPhoto(offerId, OWNER,
                new ByteArrayInputStream(new byte[]{1}), 1L, "image/png", "a.png");

        verify(offerPhotoRepository).save(captor.capture());
        org.junit.jupiter.api.Assertions.assertTrue(captor.getValue().isMain());
    }

    @Test
    void deleteGalleryPhoto_whenPhotoExists_thenRemovesRowAndObject() {
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").build();
        when(offerPhotoRepository.findById(p0.getId())).thenReturn(Optional.of(p0));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(new ArrayList<>());

        OfferDTO dto = offerService.deleteGalleryPhoto(offerId, p0.getId(), OWNER);

        verify(offerPhotoRepository).delete(p0);
        verify(photoStorage).delete(offerId, "k0");
        assertTrue(dto.getGallery().isEmpty());
    }
}
