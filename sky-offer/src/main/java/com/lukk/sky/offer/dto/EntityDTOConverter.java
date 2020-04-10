package com.lukk.sky.offer.dto;

import com.lukk.sky.offer.entity.Offer;
import com.lukk.sky.offer.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.validation.ValidationException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EntityDTOConverter {

    private final OfferRepository offerRepository;

    public OfferDTO convertOfferEntity_toDTO(Offer offer) {
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
                .booked(offer.getBooked())
                .photoPath(offer.getPhotoPath())
                .build();
    }

    public Offer convertNewOfferDTO_toEntity(OfferDTO offerDTO) {

        return Offer.builder()
                .hotelName(offerDTO.getHotelName())
                .city(offerDTO.getCity())
                .country(offerDTO.getCountry())
                .ownerEmail(offerDTO.getOwnerEmail())
                .description(offerDTO.getDescription())
                .comment(offerDTO.getComment())
                .price(offerDTO.getPrice())
                .roomCapacity(offerDTO.getRoomCapacity())
                .booked(offerDTO.getBooked())
                .photoPath(offerDTO.getPhotoPath())
                .build();
    }

    public Offer convertEditedOfferDTO_ToEntity(OfferDTO offerDTO) {
        Offer dbOffer = offerRepository
                .findById(offerDTO.getId())
                .orElseThrow(() -> new ValidationException("Can't edit offer without its ID."));

        return createOffer_FromOnlyNotNull_OfferDTOProperties(offerDTO, dbOffer);
    }

    public Offer createOffer_FromOnlyNotNull_OfferDTOProperties(OfferDTO offerDTO, Offer dbOffer) {
        Offer.OfferBuilder builder = Offer.builder();

        builder.hotelName(Optional.ofNullable(offerDTO.getHotelName()).orElse(dbOffer.getHotelName()));
        builder.city(Optional.ofNullable(offerDTO.getCity()).orElse(dbOffer.getCity()));
        builder.country(Optional.ofNullable(offerDTO.getCountry()).orElse(dbOffer.getCountry()));
        builder.ownerEmail(Optional.ofNullable(offerDTO.getOwnerEmail()).orElse(dbOffer.getOwnerEmail()));
        builder.description(Optional.ofNullable(offerDTO.getDescription()).orElse(dbOffer.getDescription()));
        builder.comment(Optional.ofNullable(offerDTO.getComment()).orElse(dbOffer.getComment()));
        builder.price(Optional.ofNullable(offerDTO.getPrice()).orElse(dbOffer.getPrice()));
        builder.roomCapacity(Optional.ofNullable(offerDTO.getRoomCapacity()).orElse(dbOffer.getRoomCapacity()));
        builder.booked(Optional.ofNullable(offerDTO.getBooked()).orElse(dbOffer.getBooked()));
        builder.photoPath(Optional.ofNullable(offerDTO.getPhotoPath()).orElse(dbOffer.getPhotoPath()));

        return builder.build();
    }

}
