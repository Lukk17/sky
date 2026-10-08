package com.lukk.sky.offer.adapters.inbound.api;

import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.adapters.dto.PhotoDTO;
import com.lukk.sky.offer.domain.ports.inbound.GalleryPhotoView;
import com.lukk.sky.offer.domain.ports.inbound.OfferView;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class OfferDtoMapper {

    static OfferDTO toDto(OfferView view) {
        List<PhotoDTO> gallery = view.gallery() == null ? List.of() : view.gallery().stream()
                .map(OfferDtoMapper::toPhotoDto)
                .toList();
        return OfferDTO.builder()
                .id(view.id())
                .hotelName(view.hotelName())
                .description(view.description())
                .comment(view.comment())
                .price(view.price())
                .ownerEmail(view.ownerEmail())
                .roomCapacity(view.roomCapacity())
                .city(view.city())
                .country(view.country())
                .externalPhotoUrl(view.externalPhotoUrl())
                .photoUrl(view.photoUrl())
                .gallery(new java.util.ArrayList<>(gallery))
                .coverPhotoUrl(view.coverPhotoUrl())
                .build();
    }

    private static PhotoDTO toPhotoDto(GalleryPhotoView photo) {
        return PhotoDTO.builder().id(photo.id()).position(photo.position()).url(photo.url()).main(photo.main()).build();
    }
}
