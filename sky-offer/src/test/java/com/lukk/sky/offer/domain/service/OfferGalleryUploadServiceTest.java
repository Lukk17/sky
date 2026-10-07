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

@DisplayName("Offer gallery upload")
@ExtendWith(MockitoExtension.class)
class OfferGalleryUploadServiceTest {

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
    @DisplayName("uploadGalleryPhoto_whenFirstPhoto_thenMarkedMain")
    void uploadGalleryPhoto_whenFirstPhoto_thenMarkedMain() {
        when(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId)).thenReturn(new ArrayList<>());
        when(photoStorage.upload(any(), any(), anyLong(), any(), any())).thenReturn("offers/" + offerId + "/k-a.png");
        org.mockito.ArgumentCaptor<OfferPhoto> captor = org.mockito.ArgumentCaptor.forClass(OfferPhoto.class);

        offerService.uploadGalleryPhoto(offerId, OWNER,
                new ByteArrayInputStream(new byte[]{1}), 1L, "image/png", "a.png");

        verify(offerPhotoRepository).saveAndFlush(captor.capture());
        org.junit.jupiter.api.Assertions.assertTrue(captor.getValue().isMain());
    }

    @Test
    @DisplayName("uploadGalleryPhoto_whenCallerIsNotOwner_thenThrowsAccessDenied")
    void uploadGalleryPhoto_whenCallerIsNotOwner_thenThrowsAccessDenied() {
        assertThrows(com.lukk.sky.offer.domain.exception.OfferAccessDeniedException.class,
                () -> offerService.uploadGalleryPhoto(offerId, "stranger@sky.dev",
                        new ByteArrayInputStream(new byte[]{1}), 1L, "image/png", "a.png"));
        org.mockito.Mockito.verify(photoStorage, org.mockito.Mockito.never())
                .upload(any(), any(), anyLong(), any(), any());
    }

}
