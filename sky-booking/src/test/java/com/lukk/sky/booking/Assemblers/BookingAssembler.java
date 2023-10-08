package com.lukk.sky.booking.Assemblers;

import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.adapters.dto.BookingPayload;
import com.lukk.sky.booking.domain.model.Booking;

import java.time.LocalDate;
import java.util.List;

import static com.lukk.sky.booking.Assemblers.UserAssembler.*;


public class BookingAssembler {
    public static Long TEST_DEFAULT_BOOKED_ID = 1L;
    public static Long TEST_DEFAULT_BOOKED_ID_2 = 2L;

    public static String TEST_DEFAULT_OFFER_ID = "101";
    public static String TEST_DEFAULT_OFFER_ID_2 = "102";

    public static LocalDate TEST_DATE = LocalDate.of(2201, 6, 20);

    public static List<Booking> getPopulatedBookedList() {
        return List.of(
                getPopulatedBooked(TEST_DEFAULT_BOOKED_ID, TEST_DEFAULT_OFFER_ID,
                        TEST_OWNER_EMAIL),
                getPopulatedBooked(TEST_DEFAULT_BOOKED_ID_2, TEST_DEFAULT_OFFER_ID_2,
                        TEST_OWNER_EMAIL_2));
    }

    public static List<BookingDTO> getPopulatedBookedDTOList() {
        return List.of(
                getPopulatedBookedDTO(TEST_DEFAULT_OFFER_ID,
                        TEST_USER_EMAIL, TEST_OWNER_EMAIL),
                getPopulatedBookedDTO(TEST_DEFAULT_OFFER_ID_2,
                        TEST_USER_EMAIL, TEST_OWNER_EMAIL_2));
    }

    public static Booking getPopulatedBooked() {
        return getPopulatedBooked(TEST_DEFAULT_BOOKED_ID, TEST_DEFAULT_OFFER_ID,
                TEST_OWNER_EMAIL);
    }

    private static Booking getPopulatedBooked(Long Id, String offerId, String owner) {
        return Booking.builder()
                .id(Id)
                .offerId(offerId)
                .bookedDate(TEST_DATE)
                .bookingUser(UserAssembler.TEST_USER_EMAIL)
                .ownerEmail(owner)
                .build();
    }

    public static BookingDTO getPopulatedBookedDTO(String offerId, String bookingUser, String owner) {
        return BookingDTO.builder()
                .offerId(offerId)
                .bookedDate(TEST_DATE.toString())
                .bookingUser(bookingUser)
                .ownerEmail(owner)
                .build();
    }

    public static BookingDTO getPopulatedBookedDTO() {
        return getPopulatedBookedDTO(TEST_DEFAULT_OFFER_ID,
                TEST_USER_EMAIL, TEST_OWNER_EMAIL);
    }

    public static BookingPayload getBookingPayload() {
        return new BookingPayload(TEST_DEFAULT_BOOKED_ID.toString(), TEST_DATE.toString());
    }
}
