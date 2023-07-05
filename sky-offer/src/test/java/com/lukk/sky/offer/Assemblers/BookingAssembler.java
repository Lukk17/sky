package com.lukk.sky.offer.Assemblers;

import com.lukk.sky.offer.domain.model.Booked;
import com.lukk.sky.offer.domain.model.Offer;

import java.time.LocalDate;
import java.util.List;

import static com.lukk.sky.offer.Assemblers.OfferAssembler.TEST_DEFAULT_OFFER_ID;
import static com.lukk.sky.offer.Assemblers.UserAssembler.TEST_USER_EMAIL;

public class BookingAssembler {
    public static Long TEST_DEFAULT_BOOKED_ID = 1L;

    public static List<Booked> getEmptyBookedList() {
        return List.of(new Booked(), new Booked());
    }

    public static List<Booked> getPopulatedBookedList() {
        return List.of(
                getPopulatedBooked(TEST_USER_EMAIL, TEST_DEFAULT_BOOKED_ID),
                getPopulatedBooked(TEST_USER_EMAIL, 2L));

    }

    public static List<Booked> getPopulatedBookedList_withPopulatedOffers() {
        return List.of(
                getPopulatedBooked_withPopulatedOffer(TEST_USER_EMAIL, TEST_DEFAULT_BOOKED_ID),
                getPopulatedBooked_withPopulatedOffer(TEST_USER_EMAIL, 2L));

    }

    public static Booked getPopulatedBooked(String userEmail, Long Id) {
        return Booked.builder()
                .bookedDate(LocalDate.now())
                .offer(new Offer())
                .userEmail(userEmail)
                .id(Id)
                .build();
    }

    public static Booked getPopulatedBooked_withPopulatedOffer(String userEmail, Long Id) {
        return Booked.builder()
                .bookedDate(LocalDate.now())
                .offer(OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID))
                .userEmail(userEmail)
                .id(Id)
                .build();
    }


}
