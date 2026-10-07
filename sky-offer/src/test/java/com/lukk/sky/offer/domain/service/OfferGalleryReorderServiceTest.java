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

@DisplayName("Offer gallery reorder")
@ExtendWith(MockitoExtension.class)
class OfferGalleryReorderServiceTest {

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
    @DisplayName("reorderGalleryPhoto_whenMovingLastToFirst_thenCoverSwaps")
    void reorderGalleryPhoto_whenMovingLastToFirst_thenCoverSwaps() {
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
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").build();
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId))
                .thenReturn(new ArrayList<>(List.of(p0)));

        assertThrows(OfferNotFoundException.class,
                () -> offerService.reorderGalleryPhoto(offerId, UUID.randomUUID(), 0, OWNER));
    }

    @Test
    @DisplayName("reorderGalleryPhoto_whenPositionOutOfRange_thenRejected")
    void reorderGalleryPhoto_whenPositionOutOfRange_thenRejected() {
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").build();
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId))
                .thenReturn(new ArrayList<>(List.of(p0)));

        assertThrows(OfferException.class,
                () -> offerService.reorderGalleryPhoto(offerId, p0.getId(), 5, OWNER));
    }

    @Test
    @DisplayName("reorderGalleryPhoto_whenMovingFirstToLast_thenShiftsForward")
    void reorderGalleryPhoto_whenMovingFirstToLast_thenShiftsForward() {
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
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").build();
        List<OfferPhoto> photos = new ArrayList<>(List.of(p0));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(photos);
        when(photoStorage.presignedUrl("k0")).thenReturn("u0");

        OfferView dto = offerService.reorderGalleryPhoto(offerId, p0.getId(), 0, OWNER);

        assertEquals(0, p0.getPosition());
        assertEquals(p0.getId(), dto.gallery().get(0).id());
    }

}
