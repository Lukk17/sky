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
import java.util.UUID;
import java.util.function.Consumer;

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
        applyIfSupplied(this.hotelName, storedOffer::setHotelName);
        applyIfSupplied(this.city, storedOffer::setCity);
        applyIfSupplied(this.country, storedOffer::setCountry);
        applyIfSupplied(this.description, storedOffer::setDescription);
        applyIfSupplied(this.comment, storedOffer::setComment);
        applyIfSupplied(this.price, storedOffer::setPrice);
        applyIfSupplied(this.roomCapacity, storedOffer::setRoomCapacity);
        applyIfSupplied(this.externalPhotoUrl, storedOffer::setExternalPhotoUrl);
    }

    private static <T> void applyIfSupplied(T suppliedValue, Consumer<T> storedFieldSetter) {
        if (suppliedValue == null) {
            return;
        }

        storedFieldSetter.accept(suppliedValue);
    }
}
