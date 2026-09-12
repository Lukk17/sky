package com.lukk.sky.offer.assemblers;

import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.adapters.dto.OfferEditDTO;
import com.lukk.sky.offer.domain.model.Offer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.lukk.sky.offer.assemblers.UserAssembler.TEST_OWNER_EMAIL;

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
