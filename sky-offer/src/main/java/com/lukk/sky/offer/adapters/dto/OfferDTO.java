package com.lukk.sky.offer.adapters.dto;

import com.lukk.sky.offer.domain.model.Offer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Optional;

@Builder
@Data
@AllArgsConstructor
public class OfferDTO {

    private Long id;
    private String hotelName;
    private String description;
    private String comment;
    private BigDecimal price;
    private String ownerEmail;
    private Long roomCapacity;
    private String city;
    private String country;
    private String photoPath;

    public static OfferDTO of(Offer offer) {
        return OfferDTO.builder()
                .hotelName(offer.getHotelName())
                .id(offer.getId())
                .city(offer.getCity())
                .country(offer.getCountry())
                .ownerEmail(offer.getOwnerEmail())
                .description(offer.getDescription())
                .comment(offer.getComment())
                .price(offer.getPrice())
                .roomCapacity(offer.getRoomCapacity())
                .photoPath(offer.getPhotoPath())
                .build();
    }

    public Offer toDomain() {

        return Offer.builder()
                .id(this.getId())
                .hotelName(this.getHotelName())
                .city(this.getCity())
                .country(this.getCountry())
                .ownerEmail(this.getOwnerEmail())
                .description(this.getDescription())
                .comment(this.getComment())
                .price(this.getPrice())
                .roomCapacity(this.getRoomCapacity())
                .photoPath(this.getPhotoPath())
                .build();
    }

    public OfferDTO mergeWithDomain(Offer dbOffer) {
        OfferDTO.OfferDTOBuilder builder = OfferDTO.builder();
        builder.id(dbOffer.getId());
        builder.hotelName(Optional.ofNullable(this.getHotelName())
                .orElse(dbOffer.getHotelName()));
        builder.city(Optional.ofNullable(this.getCity())
                .orElse(dbOffer.getCity()));
        builder.country(Optional.ofNullable(this.getCountry())
                .orElse(dbOffer.getCountry()));
        builder.ownerEmail(Optional.ofNullable(this.getOwnerEmail())
                .orElse(dbOffer.getOwnerEmail()));
        builder.description(Optional.ofNullable(this.getDescription())
                .orElse(dbOffer.getDescription()));
        builder.comment(Optional.ofNullable(this.getComment())
                .orElse(dbOffer.getComment()));
        builder.price(Optional.ofNullable(this.getPrice())
                .orElse(dbOffer.getPrice()));
        builder.roomCapacity(Optional.ofNullable(this.getRoomCapacity())
                .orElse(dbOffer.getRoomCapacity()));
        builder.photoPath(Optional.ofNullable(this.getPhotoPath())
                .orElse(dbOffer.getPhotoPath()));

        return builder.build();
    }
}
