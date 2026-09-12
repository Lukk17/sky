package com.lukk.sky.offer.adapters.dto;

import com.lukk.sky.offer.adapters.dto.validation.ExternalPhotoUrl;
import com.lukk.sky.offer.adapters.dto.validation.NullOrNotBlank;
import com.lukk.sky.offer.domain.model.Offer;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Builder
@Data
@AllArgsConstructor
public class OfferEditDTO {

    @NotNull
    private UUID id;

    @NullOrNotBlank
    @Size(max = 255)
    private String hotelName;

    @Size(max = 3000)
    private String description;

    @Size(max = 1000)
    private String comment;

    @Min(value = 0)
    private BigDecimal price;

    private String ownerEmail;

    @Min(value = 1)
    private Long roomCapacity;

    @NullOrNotBlank
    @Size(max = 255)
    private String city;

    @NullOrNotBlank
    @Size(max = 255)
    private String country;

    @ExternalPhotoUrl
    private String externalPhotoUrl;

    public static OfferEditDTO of(Offer offer) {
        return OfferEditDTO.builder()
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

    public void applyTo(Offer storedOffer) {
        storedOffer.setHotelName(Objects.requireNonNullElseGet(this.hotelName, storedOffer::getHotelName));
        storedOffer.setCity(Objects.requireNonNullElseGet(this.city, storedOffer::getCity));
        storedOffer.setCountry(Objects.requireNonNullElseGet(this.country, storedOffer::getCountry));
        storedOffer.setDescription(Objects.requireNonNullElseGet(this.description, storedOffer::getDescription));
        storedOffer.setComment(Objects.requireNonNullElseGet(this.comment, storedOffer::getComment));
        storedOffer.setPrice(Objects.requireNonNullElseGet(this.price, storedOffer::getPrice));
        storedOffer.setRoomCapacity(Objects.requireNonNullElseGet(this.roomCapacity, storedOffer::getRoomCapacity));
        storedOffer.setExternalPhotoUrl(
                Objects.requireNonNullElseGet(this.externalPhotoUrl, storedOffer::getExternalPhotoUrl));
    }
}
