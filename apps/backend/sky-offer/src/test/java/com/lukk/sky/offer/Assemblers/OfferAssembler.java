package com.lukk.sky.offer.assemblers;

import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.adapters.dto.OfferEditDTO;
import com.lukk.sky.offer.adapters.dto.PhotoDTO;
import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.ports.inbound.CreateOfferCommand;
import com.lukk.sky.offer.domain.ports.inbound.EditOfferCommand;
import com.lukk.sky.offer.domain.ports.inbound.GalleryPhotoView;
import com.lukk.sky.offer.domain.ports.inbound.OfferView;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.lukk.sky.common.test.TestUsers.TEST_OWNER_EMAIL;

public class OfferAssembler {
    public static final String TEST_HOTEL_NAME = "testHotelName";
    public static final String TEST_CITY = "testCity";
    public static final String TEST_COUNTRY = "testCountry";
    public static final String TEST_COMMENT = "testComment";
    public static final String TEST_DESCRIPTION = "testDescription";
    public static final String TEST_EXTERNAL_PHOTO_URL = "https://images.example.com/test-hotel.jpeg";
    public static final BigDecimal TEST_PRICE = BigDecimal.valueOf(20);
    public static final Long TEST_ROOM_CAPACITY = 5L;

    public static final UUID TEST_DEFAULT_OFFER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID TEST_DEFAULT_OFFER_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

    public static String testPhotoObjectKey(UUID offerId) {
        return "offers/" + offerId + "/3f2a1c88-5d24-4b6e-9c0f-1a2b3c4d5e6f-hotel.png";
    }

    public static List<Offer> getPopulatedOffers() {
        return List.of(
                getPopulatedOffer(TEST_DEFAULT_OFFER_ID),
                getPopulatedOffer(TEST_DEFAULT_OFFER_ID_2)
        );
    }

    public static Offer getPopulatedOffer(UUID id) {
        return Offer.builder()
                .hotelName(TEST_HOTEL_NAME)
                .ownerEmail(TEST_OWNER_EMAIL)
                .city(TEST_CITY)
                .comment(TEST_COMMENT)
                .country(TEST_COUNTRY)
                .description(TEST_DESCRIPTION)
                .photoObjectKey(testPhotoObjectKey(id))
                .externalPhotoUrl(TEST_EXTERNAL_PHOTO_URL)
                .id(id)
                .roomCapacity(TEST_ROOM_CAPACITY)
                .price(TEST_PRICE)
                .build();
    }

    public static List<OfferDTO> getPopulatedOffersDTO() {

        return List.of(
                getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID),
                getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID_2)
        );
    }

    public static OfferDTO getPopulatedOfferDTO(UUID id) {
        return OfferDTO.builder()
                .hotelName(TEST_HOTEL_NAME)
                .city(TEST_CITY)
                .country(TEST_COUNTRY)
                .comment(TEST_COMMENT)
                .description(TEST_DESCRIPTION)
                .id(id)
                .ownerEmail(TEST_OWNER_EMAIL)
                .externalPhotoUrl(TEST_EXTERNAL_PHOTO_URL)
                .price(TEST_PRICE)
                .roomCapacity(TEST_ROOM_CAPACITY)
                .build();
    }

    public static CreateOfferCommand toCreateCommand(OfferDTO dto) {
        return new CreateOfferCommand(dto.getHotelName(), dto.getDescription(), dto.getComment(),
                dto.getPrice(), dto.getOwnerEmail(), dto.getRoomCapacity(),
                dto.getCity(), dto.getCountry(), dto.getExternalPhotoUrl());
    }

    public static EditOfferCommand toEditCommand(OfferEditDTO dto) {
        return new EditOfferCommand(dto.getId(), dto.getHotelName(), dto.getCity(), dto.getCountry(),
                dto.getDescription(), dto.getComment(), dto.getPrice(),
                dto.getRoomCapacity(), dto.getExternalPhotoUrl());
    }

    public static OfferDTO toOfferDTO(OfferView view) {
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
                .gallery(view.gallery() == null ? new java.util.ArrayList<>()
                        : new java.util.ArrayList<>(view.gallery().stream()
                                .map(photo -> PhotoDTO.builder().id(photo.id()).position(photo.position())
                                        .url(photo.url()).main(photo.main()).build())
                                .toList()))
                .coverPhotoUrl(view.coverPhotoUrl())
                .build();
    }

    public static OfferView toOfferView(OfferDTO dto) {
        return new OfferView(dto.getId(), dto.getHotelName(), dto.getDescription(), dto.getComment(),
                dto.getPrice(), dto.getOwnerEmail(), dto.getRoomCapacity(), dto.getCity(), dto.getCountry(),
                dto.getExternalPhotoUrl(), dto.getPhotoUrl(),
                dto.getGallery() == null ? List.of() : dto.getGallery().stream()
                        .map(photo -> new GalleryPhotoView(photo.getId(), photo.getPosition(),
                                photo.getUrl(), photo.isMain()))
                        .toList(),
                dto.getCoverPhotoUrl());
    }

    public static List<OfferView> toOfferViews(List<OfferDTO> dtos) {
        return dtos.stream().map(OfferAssembler::toOfferView).toList();
    }

    public static OfferEditDTO getPopulatedOfferEditDTO(UUID id) {
        return OfferEditDTO.builder()
                .hotelName(TEST_HOTEL_NAME)
                .city(TEST_CITY)
                .country(TEST_COUNTRY)
                .comment(TEST_COMMENT)
                .description(TEST_DESCRIPTION)
                .id(id)
                .ownerEmail(TEST_OWNER_EMAIL)
                .externalPhotoUrl(TEST_EXTERNAL_PHOTO_URL)
                .price(TEST_PRICE)
                .roomCapacity(TEST_ROOM_CAPACITY)
                .build();
    }
}
