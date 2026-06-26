package com.lukk.sky.offer.adapters.dto;

import com.lukk.sky.offer.domain.model.Offer;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Builder
@Data
@AllArgsConstructor
public class OfferDTO {

    private UUID id;

    @NotBlank
    private String hotelName;

    @Size(max = 3000)
    private String description;

    @Size(max = 1000)
    private String comment;

    @NotNull
    @Min(value = 0)
    private BigDecimal price;

    @Email
    private String ownerEmail;

    @NotNull
    @Min(value = 1)
    private Long roomCapacity;

    @NotBlank
    private String city;

    @NotBlank
    private String country;
    private String photoPath;

    /** Presigned GET URL, populated by the service layer when returning DTOs to callers. */
    private String photoUrl;

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
        builder.hotelName(Objects.requireNonNullElseGet(this.getHotelName(), dbOffer::getHotelName));
        builder.city(Objects.requireNonNullElseGet(this.getCity(), dbOffer::getCity));
        builder.country(Objects.requireNonNullElseGet(this.getCountry(), dbOffer::getCountry));
        builder.ownerEmail(Objects.requireNonNullElseGet(this.getOwnerEmail(), dbOffer::getOwnerEmail));
        builder.description(Objects.requireNonNullElseGet(this.getDescription(), dbOffer::getDescription));
        builder.comment(Objects.requireNonNullElseGet(this.getComment(), dbOffer::getComment));
        builder.price(Objects.requireNonNullElseGet(this.getPrice(), dbOffer::getPrice));
        builder.roomCapacity(Objects.requireNonNullElseGet(this.getRoomCapacity(), dbOffer::getRoomCapacity));
        builder.photoPath(Objects.requireNonNullElseGet(this.getPhotoPath(), dbOffer::getPhotoPath));

        return builder.build();
    }
}
