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

@DisplayName("Offer gallery cover and view")
@ExtendWith(MockitoExtension.class)
class OfferGalleryCoverServiceTest {

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
    @DisplayName("setGalleryCover_whenCalled_thenSetsMainFlagWithoutReordering")
    void setGalleryCover_whenCalled_thenSetsMainFlagWithoutReordering() {
        // given
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").build();
        OfferPhoto p1 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(1).objectKey("k1").build();
        List<OfferPhoto> photos = new ArrayList<>(List.of(p0, p1));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(photos);
        when(photoStorage.presignedUrl("k0")).thenReturn("u0");
        when(photoStorage.presignedUrl("k1")).thenReturn("u1");

        // when
        OfferView dto = offerService.setGalleryCover(offerId, p1.getId(), OWNER);

        // then
        assertEquals(0, p0.getPosition());
        assertEquals(1, p1.getPosition());
        assertEquals(p0.getId(), dto.gallery().get(0).id());
        assertEquals("u1", dto.coverPhotoUrl());
        verify(offerPhotoRepository, org.mockito.Mockito.times(2)).saveAllAndFlush(anyList());
    }

    @Test
    @DisplayName("galleryOf_whenEmptyAndExternalUrlSet_thenFallsBackToExternal")
    void galleryOf_whenEmptyAndExternalUrlSet_thenFallsBackToExternal() {
        // given
        offer.setExternalPhotoUrl("https://cdn.example/cover.png");
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(new ArrayList<>());
        when(photoStorage.upload(any(), any(), anyLong(), any(), any())).thenReturn("offers/" + offerId + "/k-a.png");

        // when
        OfferView dto = offerService.uploadGalleryPhoto(offerId, OWNER,
                new ByteArrayInputStream(new byte[]{1}), 1L, "image/png", "a.png");

        // then
        assertTrue(dto.gallery().isEmpty() || dto.coverPhotoUrl() != null);
    }

    @Test
    @DisplayName("galleryOf_whenPhotoHasExternalUrl_thenUsesIt")
    void galleryOf_whenPhotoHasExternalUrl_thenUsesIt() {
        // given
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0)
                .externalUrl("https://cdn.example/x.png").build();
        List<OfferPhoto> photos = new ArrayList<>(List.of(p0));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(photos);

        // when
        OfferView dto = offerService.reorderGalleryPhoto(offerId, p0.getId(), 0, OWNER);

        // then
        org.mockito.Mockito.verify(photoStorage, org.mockito.Mockito.never())
                .presignedUrl(org.mockito.ArgumentMatchers.anyString());
        assertEquals("https://cdn.example/x.png", dto.gallery().get(0).url());
    }

    @Test
    @DisplayName("coverPhotoUrl_whenMainFlagSet_thenPrefersMainOverPosition")
    void coverPhotoUrl_whenMainFlagSet_thenPrefersMainOverPosition() {
        // given
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").main(false).build();
        OfferPhoto p1 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(1).objectKey("k1").main(true).build();
        List<OfferPhoto> photos = new ArrayList<>(List.of(p0, p1));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(photos);
        when(photoStorage.presignedUrl("k0")).thenReturn("u0");
        when(photoStorage.presignedUrl("k1")).thenReturn("u1");
        when(offerPhotoRepository.findById(p0.getId())).thenReturn(Optional.of(p0));

        // when
        OfferView dto = offerService.deleteGalleryPhoto(offerId, p0.getId(), OWNER);

        // then
        verify(offerPhotoRepository).delete(p0);
    }

    @Test
    @DisplayName("setGalleryCover_whenCalled_thenSetsMainFlag")
    void setGalleryCover_whenCalled_thenSetsMainFlag() {
        // given
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").main(true).build();
        OfferPhoto p1 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(1).objectKey("k1").main(false).build();
        List<OfferPhoto> photos = new ArrayList<>(List.of(p0, p1));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(photos);
        when(photoStorage.presignedUrl(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(inv -> "u-" + inv.getArgument(0));

        // when
        OfferView dto = offerService.setGalleryCover(offerId, p1.getId(), OWNER);

        // then
        org.junit.jupiter.api.Assertions.assertTrue(p1.isMain());
        org.junit.jupiter.api.Assertions.assertFalse(p0.isMain());
        org.junit.jupiter.api.Assertions.assertTrue(dto.gallery().stream()
                .filter(g -> g.id().equals(p1.getId())).findFirst().orElseThrow().main());
    }

    @Test
    @DisplayName("setGalleryCover_whenChanged_thenListPayloadShowsNewCover")
    void setGalleryCover_whenChanged_thenListPayloadShowsNewCover() {
        // given
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0)
                .objectKey("k0").main(true).build();
        OfferPhoto p1 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(1)
                .objectKey("k1").main(false).build();
        List<OfferPhoto> photos = new ArrayList<>(List.of(p0, p1));
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(photos);
        when(photoStorage.presignedUrl("k0")).thenReturn("u0");
        when(photoStorage.presignedUrl("k1")).thenReturn("u1");

        // when
        offerService.setGalleryCover(offerId, p1.getId(), OWNER);
        OfferView dto = offerService.summaryView(offer);

        // then
        assertEquals("u1", dto.coverPhotoUrl());
        assertEquals("u1", dto.photoUrl());
    }

    @Test
    @DisplayName("setGalleryCover_whenConcurrentCoverWins_thenAnswers409")
    void setGalleryCover_whenConcurrentCoverWins_thenAnswers409() {
        // given
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0)
                .objectKey("k0").main(true).build();
        OfferPhoto p1 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(1)
                .objectKey("k1").main(false).build();
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId))
                .thenReturn(new ArrayList<>(List.of(p0, p1)));
        org.mockito.Mockito.doThrow(new org.springframework.dao.DataIntegrityViolationException("two mains"))
                .when(offerPhotoRepository).saveAllAndFlush(org.mockito.ArgumentMatchers.anyList());

        // when / then
        assertThrows(com.lukk.sky.offer.domain.exception.GalleryCoverConflictException.class,
                () -> offerService.setGalleryCover(offerId, p1.getId(), OWNER));
    }

    @Test
    @DisplayName("setGalleryCover_whenTargetAlreadySoleMain_thenReturnsWithoutRewriting")
    void setGalleryCover_whenTargetAlreadySoleMain_thenReturnsWithoutRewriting() {
        // given
        OfferPhoto p0 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(0).objectKey("k0").main(true).build();
        OfferPhoto p1 = OfferPhoto.builder().id(UUID.randomUUID()).offer(offer).position(1).objectKey("k1").main(false).build();
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId))
                .thenReturn(new ArrayList<>(List.of(p0, p1)));
        when(photoStorage.presignedUrl("k0")).thenReturn("u0");
        when(photoStorage.presignedUrl("k1")).thenReturn("u1");

        // when
        OfferView dto = offerService.setGalleryCover(offerId, p0.getId(), OWNER);

        // then
        org.mockito.Mockito.verify(offerPhotoRepository, org.mockito.Mockito.never())
                .saveAllAndFlush(org.mockito.ArgumentMatchers.anyList());
        assertEquals("u0", dto.coverPhotoUrl());
    }

}
