package com.lukk.sky.booking.Assemblers;

import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.adapters.dto.BookingPayload;
import com.lukk.sky.booking.domain.model.Booking;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.lukk.sky.booking.Assemblers.UserAssembler.TEST_OWNER_EMAIL;
import static com.lukk.sky.booking.Assemblers.UserAssembler.TEST_OWNER_EMAIL_2;
import static com.lukk.sky.booking.Assemblers.UserAssembler.TEST_USER_EMAIL;


public class BookingAssembler {
    public static UUID TEST_DEFAULT_BOOKED_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static UUID TEST_DEFAULT_BOOKED_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

    public static UUID TEST_DEFAULT_OFFER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    public static UUID TEST_DEFAULT_OFFER_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000102");

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

    private static Booking getPopulatedBooked(UUID id, UUID offerId, String owner) {
        return Booking.builder()
                .id(id)
                .offerId(offerId)
                .bookedDate(TEST_DATE)
                .bookingUser(UserAssembler.TEST_USER_EMAIL)
                .ownerEmail(owner)
                .build();
    }

    public static BookingDTO getPopulatedBookedDTO(UUID offerId, String bookingUser, String owner) {
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
        return new BookingPayload(TEST_DEFAULT_OFFER_ID, TEST_DATE.toString());
    }
}
