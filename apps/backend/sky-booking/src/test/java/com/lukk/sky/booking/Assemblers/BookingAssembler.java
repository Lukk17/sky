package com.lukk.sky.booking.assemblers;

import com.lukk.sky.booking.domain.model.Booking;
import com.lukk.sky.booking.domain.ports.inbound.BookingView;
import com.lukk.sky.contract.dto.BookingDTO;
import com.lukk.sky.contract.dto.BookingPayload;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.lukk.sky.common.test.TestUsers.TEST_OWNER_EMAIL;
import static com.lukk.sky.common.test.TestUsers.TEST_OWNER_EMAIL_2;
import static com.lukk.sky.common.test.TestUsers.TEST_USER_EMAIL;
import static com.lukk.sky.common.web.DateTimeConstants.DATE_FORMAT;

public class BookingAssembler {

    public static final UUID TEST_DEFAULT_BOOKED_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID TEST_DEFAULT_BOOKED_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

    public static final UUID TEST_DEFAULT_OFFER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    public static final UUID TEST_DEFAULT_OFFER_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000102");

    public static final LocalDate TEST_DATE = LocalDate.of(2201, 6, 20);

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
                .bookingUser(TEST_USER_EMAIL)
                .ownerEmail(owner)
                .build();
    }

    public static BookingDTO getPopulatedBookedDTO(UUID offerId, String bookingUser, String owner) {
        BookingDTO dto = new BookingDTO();
        dto.setOfferId(offerId);
        dto.setBookedDate(TEST_DATE);
        dto.setBookingUser(bookingUser);
        dto.setOwnerEmail(owner);
        return dto;
    }

    public static BookingDTO getPopulatedBookedDTO() {
        return getPopulatedBookedDTO(TEST_DEFAULT_OFFER_ID,
                TEST_USER_EMAIL, TEST_OWNER_EMAIL);
    }

    public static BookingPayload getBookingPayload() {
        BookingPayload payload = new BookingPayload();
        payload.setOfferId(TEST_DEFAULT_OFFER_ID);
        payload.setDateToBook(TEST_DATE);
        return payload;
    }

    public static BookingView toBookingView(BookingDTO dto) {
        return new BookingView(dto.getId(), dto.getOfferId(), dto.getBookedDate().toString(),
                dto.getBookingUser(), dto.getOwnerEmail());
    }

    public static BookingDTO toBookingDTO(BookingView view) {
        BookingDTO dto = new BookingDTO();
        dto.setId(view.id());
        dto.setOfferId(view.offerId());
        dto.setBookedDate(LocalDate.parse(view.bookedDate(), DATE_FORMAT));
        dto.setBookingUser(view.bookingUser());
        dto.setOwnerEmail(view.ownerEmail());
        return dto;
    }
}
