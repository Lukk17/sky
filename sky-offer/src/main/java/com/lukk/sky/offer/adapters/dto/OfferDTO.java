package com.lukk.sky.offer.adapters.dto;

import com.lukk.sky.offer.adapters.dto.validation.ExternalPhotoUrl;
import com.lukk.sky.offer.domain.model.Offer;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Data
@AllArgsConstructor
public class OfferDTO {

    private UUID id;

    @NotBlank
    @Size(max = 255)
    private String hotelName;

    @Size(max = 3000)
    private String description;

    @Size(max = 1000)
    private String comment;

    @NotNull
    @Min(value = 0)
    private BigDecimal price;

    private String ownerEmail;

    @NotNull
    @Min(value = 1)
    private Long roomCapacity;

    @NotBlank
    @Size(max = 255)
    private String city;

    @NotBlank
    @Size(max = 255)
    private String country;

    @ExternalPhotoUrl
    private String externalPhotoUrl;

    /**
     * Address a client fetches the photo from, derived per response and never persisted.
     */
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
                .externalPhotoUrl(offer.getExternalPhotoUrl())
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
                .externalPhotoUrl(this.getExternalPhotoUrl())
                .build();
    }

}
