package com.lukk.sky.offer.adapters.dto;

import com.lukk.sky.offer.assemblers.OfferAssembler;
import com.lukk.sky.offer.domain.model.Offer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.lukk.sky.offer.assemblers.OfferAssembler.TEST_CITY;
import static com.lukk.sky.offer.assemblers.OfferAssembler.TEST_COMMENT;
import static com.lukk.sky.offer.assemblers.OfferAssembler.TEST_COUNTRY;
import static com.lukk.sky.offer.assemblers.OfferAssembler.TEST_DEFAULT_OFFER_ID;
import static com.lukk.sky.offer.assemblers.OfferAssembler.TEST_DESCRIPTION;
import static com.lukk.sky.offer.assemblers.OfferAssembler.TEST_EXTERNAL_PHOTO_URL;
import static com.lukk.sky.offer.assemblers.OfferAssembler.TEST_HOTEL_NAME;
import static com.lukk.sky.offer.assemblers.OfferAssembler.TEST_PRICE;
import static com.lukk.sky.offer.assemblers.OfferAssembler.TEST_ROOM_CAPACITY;
import static com.lukk.sky.offer.assemblers.OfferAssembler.testPhotoObjectKey;
import static com.lukk.sky.offer.assemblers.UserAssembler.SECOND_TEST_USER_EMAIL;
import static com.lukk.sky.offer.assemblers.UserAssembler.TEST_OWNER_EMAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("OfferEditDTO: merging a partial payload onto the stored offer")
class OfferEditDTOTest {

    private static final String UPDATED_NAME = "UpdatedName";

    @Test
    @DisplayName("applyTo_whenDescriptionIsAbsentOnBothThePayloadAndTheStoredOffer_thenLeaveItNull")
    void applyTo_whenDescriptionIsAbsentOnBothSides_thenLeaveItNull() {
        // given
        Offer storedOffer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        storedOffer.setDescription(null);
        OfferEditDTO partialEdit = onlyHotelName();

        // when
        partialEdit.applyTo(storedOffer);

        // then
        assertNull(storedOffer.getDescription());
        assertEquals(UPDATED_NAME, storedOffer.getHotelName());
    }

    @Test
    @DisplayName("applyTo_whenCommentIsAbsentOnBothThePayloadAndTheStoredOffer_thenLeaveItNull")
    void applyTo_whenCommentIsAbsentOnBothSides_thenLeaveItNull() {
        // given
        Offer storedOffer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        storedOffer.setComment(null);
        OfferEditDTO partialEdit = onlyHotelName();

        // when
        partialEdit.applyTo(storedOffer);

        // then
        assertNull(storedOffer.getComment());
        assertEquals(UPDATED_NAME, storedOffer.getHotelName());
    }

    @Test
    @DisplayName("applyTo_whenExternalPhotoUrlIsAbsentOnBothThePayloadAndTheStoredOffer_thenLeaveItNull")
    void applyTo_whenExternalPhotoUrlIsAbsentOnBothSides_thenLeaveItNull() {
        // given
        Offer storedOffer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        storedOffer.setExternalPhotoUrl(null);
        OfferEditDTO partialEdit = onlyHotelName();

        // when
        partialEdit.applyTo(storedOffer);

        // then
        assertNull(storedOffer.getExternalPhotoUrl());
        assertEquals(UPDATED_NAME, storedOffer.getHotelName());
    }

    @Test
    @DisplayName("applyTo_whenEveryOptionalFieldIsAbsentOnBothSides_thenLeaveThemAllNull")
    void applyTo_whenEveryOptionalFieldIsAbsentOnBothSides_thenLeaveThemAllNull() {
        // given
        Offer storedOffer = sparseOffer();
        OfferEditDTO partialEdit = onlyHotelName();

        // when
        partialEdit.applyTo(storedOffer);

        // then
        assertNull(storedOffer.getDescription());
        assertNull(storedOffer.getComment());
        assertNull(storedOffer.getExternalPhotoUrl());
        assertEquals(UPDATED_NAME, storedOffer.getHotelName());
    }

    @Test
    @DisplayName("applyTo_whenAFieldIsAbsentFromThePayload_thenKeepTheStoredValue")
    void applyTo_whenAFieldIsAbsentFromThePayload_thenKeepTheStoredValue() {
        // given
        Offer storedOffer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        OfferEditDTO partialEdit = onlyHotelName();

        // when
        partialEdit.applyTo(storedOffer);

        // then
        assertEquals(UPDATED_NAME, storedOffer.getHotelName());
        assertEquals(TEST_CITY, storedOffer.getCity());
        assertEquals(TEST_COUNTRY, storedOffer.getCountry());
        assertEquals(TEST_DESCRIPTION, storedOffer.getDescription());
        assertEquals(TEST_COMMENT, storedOffer.getComment());
        assertEquals(TEST_PRICE, storedOffer.getPrice());
        assertEquals(TEST_ROOM_CAPACITY, storedOffer.getRoomCapacity());
        assertEquals(TEST_EXTERNAL_PHOTO_URL, storedOffer.getExternalPhotoUrl());
    }

    @Test
    @DisplayName("applyTo_whenEveryFieldIsSupplied_thenOverwriteEveryStoredValue")
    void applyTo_whenEveryFieldIsSupplied_thenOverwriteEveryStoredValue() {
        // given
        Offer storedOffer = sparseOffer();
        OfferEditDTO fullEdit = OfferAssembler.getPopulatedOfferEditDTO(TEST_DEFAULT_OFFER_ID);
        fullEdit.setHotelName(UPDATED_NAME);

        // when
        fullEdit.applyTo(storedOffer);

        // then
        assertEquals(UPDATED_NAME, storedOffer.getHotelName());
        assertEquals(TEST_CITY, storedOffer.getCity());
        assertEquals(TEST_COUNTRY, storedOffer.getCountry());
        assertEquals(TEST_DESCRIPTION, storedOffer.getDescription());
        assertEquals(TEST_COMMENT, storedOffer.getComment());
        assertEquals(TEST_PRICE, storedOffer.getPrice());
        assertEquals(TEST_ROOM_CAPACITY, storedOffer.getRoomCapacity());
        assertEquals(TEST_EXTERNAL_PHOTO_URL, storedOffer.getExternalPhotoUrl());
    }

    @Test
    @DisplayName("applyTo_whenThePayloadNamesAnotherOwner_thenTheStoredOwnerAndPhotoKeyAreUntouched")
    void applyTo_whenThePayloadNamesAnotherOwner_thenTheStoredOwnerAndPhotoKeyAreUntouched() {
        // given
        Offer storedOffer = sparseOffer();
        String storedKey = testPhotoObjectKey(TEST_DEFAULT_OFFER_ID);
        storedOffer.setPhotoObjectKey(storedKey);
        OfferEditDTO partialEdit = onlyHotelName();
        partialEdit.setOwnerEmail(SECOND_TEST_USER_EMAIL);

        // when
        partialEdit.applyTo(storedOffer);

        // then
        assertEquals(TEST_OWNER_EMAIL, storedOffer.getOwnerEmail());
        assertEquals(storedKey, storedOffer.getPhotoObjectKey());
    }

    private static Offer sparseOffer() {
        return Offer.builder()
                .id(TEST_DEFAULT_OFFER_ID)
                .hotelName(TEST_HOTEL_NAME)
                .ownerEmail(TEST_OWNER_EMAIL)
                .city(TEST_CITY)
                .country(TEST_COUNTRY)
                .price(BigDecimal.valueOf(20))
                .roomCapacity(TEST_ROOM_CAPACITY)
                .build();
    }

    private static OfferEditDTO onlyHotelName() {
        return OfferEditDTO.builder()
                .id(TEST_DEFAULT_OFFER_ID)
                .hotelName(UPDATED_NAME)
                .build();
    }
}
