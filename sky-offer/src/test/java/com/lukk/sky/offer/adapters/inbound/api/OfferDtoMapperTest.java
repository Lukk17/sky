package com.lukk.sky.offer.adapters.inbound.api;

import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.domain.ports.inbound.GalleryPhotoView;
import com.lukk.sky.offer.domain.ports.inbound.OfferView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("OfferDtoMapper: OfferView to OfferDTO mapping")
class OfferDtoMapperTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    @DisplayName("toDto_whenGalleryIsNull_thenReturnsEmptyGallery")
    void toDto_whenGalleryIsNull_thenReturnsEmptyGallery() {
        // given
        OfferView view = new OfferView(ID, "H", "D", "C", BigDecimal.TEN,
                "owner@sky.dev", 2L, "City", "Country", null, null, null, null);

        // when
        OfferDTO dto = OfferDtoMapper.toDto(view);

        // then
        assertEquals(ID, dto.getId());
        assertTrue(dto.getGallery().isEmpty());
    }

    @Test
    @DisplayName("toDto_whenGalleryHasPhotos_thenMapsPositionsUrlsAndMainFlags")
    void toDto_whenGalleryHasPhotos_thenMapsPositionsUrlsAndMainFlags() {
        // given
        UUID photoId = UUID.randomUUID();
        OfferView view = new OfferView(ID, "H", "D", "C", BigDecimal.TEN,
                "owner@sky.dev", 2L, "City", "Country", null, "https://cdn.example/cover.png",
                List.of(new GalleryPhotoView(photoId, 0, "https://cdn.example/cover.png", true)),
                "https://cdn.example/cover.png");

        // when
        OfferDTO dto = OfferDtoMapper.toDto(view);

        // then
        assertEquals(1, dto.getGallery().size());
        assertEquals(photoId, dto.getGallery().get(0).getId());
        assertEquals(0, dto.getGallery().get(0).getPosition());
        assertEquals("https://cdn.example/cover.png", dto.getGallery().get(0).getUrl());
        assertTrue(dto.getGallery().get(0).isMain());
        assertEquals("https://cdn.example/cover.png", dto.getCoverPhotoUrl());
    }
}
