package com.lukk.sky.offer.Assemblers;

import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.domain.model.Offer;

import java.math.BigDecimal;
import java.util.List;

import static com.lukk.sky.offer.Assemblers.UserAssembler.TEST_USER_EMAIL;

public class OfferAssembler {
    public static String TEST_HOTEL_NAME = "testHotelName";
    public static String TEST_CITY = "testCity";
    public static String TEST_COUNTRY = "testCountry";
    public static String TEST_COMMENT = "testComment";
    public static String TEST_DESCRIPTION = "testDescription";
    public static String TEST_PHOTO_PATH = "testPhotoPath";
    public static BigDecimal TEST_PRICE = BigDecimal.valueOf(20);
    public static Long TEST_ROOM_CAPACITY = 5L;

    public static Long TEST_DEFAULT_OFFER_ID = 1L;


    public static List<Offer> getPopulatedOffers() {
        return List.of(
                getPopulatedOffer(TEST_DEFAULT_OFFER_ID),
                getPopulatedOffer(2L)
        );
    }

    public static Offer getPopulatedOffer(Long id) {
        return Offer.builder()
                .hotelName(TEST_HOTEL_NAME)
                .ownerEmail(TEST_USER_EMAIL)
                .city(TEST_CITY)
                .comment(TEST_COMMENT)
                .country(TEST_COUNTRY)
                .description(TEST_DESCRIPTION)
                .photoPath(TEST_PHOTO_PATH)
                .id(id)
                .roomCapacity(TEST_ROOM_CAPACITY)
                .price(TEST_PRICE)
                .build();
    }

    public static Offer getEmptyOffer(Long id) {
        return Offer.builder()
                .hotelName("")
                .ownerEmail("")
                .city("")
                .comment("")
                .country("")
                .description("")
                .photoPath("")
                .id(id)
                .roomCapacity(1L)
                .price(BigDecimal.valueOf(1))
                .build();
    }


    public static List<OfferDTO> getPopulatedOffersDTO() {

        return List.of(
                getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID),
                getPopulatedOfferDTO(2L)
        );
    }

    public static OfferDTO getPopulatedOfferDTO(Long id) {
        return OfferDTO.builder()
                .hotelName(TEST_HOTEL_NAME)
                .city(TEST_CITY)
                .country(TEST_COUNTRY)
                .comment(TEST_COMMENT)
                .description(TEST_DESCRIPTION)
                .id(id)
                .ownerEmail(TEST_USER_EMAIL)
                .photoPath(TEST_PHOTO_PATH)
                .price(TEST_PRICE)
                .roomCapacity(TEST_ROOM_CAPACITY)
                .build();
    }
}
