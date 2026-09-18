package com.lukk.sky.booking.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Booking identity contract")
class BookingTest {

    private static final UUID BOOKING_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID OFFER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000bb");
    private static final LocalDate BOOKED_DATE = LocalDate.of(2201, 6, 20);
    private static final String BOOKING_USER = "user@sky.dev";
    private static final String OWNER_EMAIL = "owner@sky.dev";

    @Test
    @DisplayName("equals is false for two unsaved bookings even when every business field matches")
    void equals_whenBothBookingsAreUnsaved_thenNotEqual() {
        Booking one = booking(null, OWNER_EMAIL);
        Booking other = booking(null, OWNER_EMAIL);

        assertThat(one).isNotEqualTo(other);
    }

    @Test
    @DisplayName("equals is true for the same identifier even when the owner email differs")
    void equals_whenSameIdentifierAndDifferentOwnerEmail_thenEqual() {
        Booking one = booking(BOOKING_ID, OWNER_EMAIL);
        Booking other = booking(BOOKING_ID, "someone.else@sky.dev");

        assertThat(one).isEqualTo(other);
    }

    @Test
    @DisplayName("equals is false for different identifiers")
    void equals_whenDifferentIdentifiers_thenNotEqual() {
        Booking one = booking(BOOKING_ID, OWNER_EMAIL);
        Booking other = booking(UUID.randomUUID(), OWNER_EMAIL);

        assertThat(one).isNotEqualTo(other);
    }

    @Test
    @DisplayName("hashCode does not change when the identifier is assigned on persist")
    void hashCode_whenIdentifierIsAssignedAfterInsertionIntoASet_thenTheEntityIsStillFound() {
        Booking booking = booking(null, OWNER_EMAIL);
        Set<Booking> bookings = new HashSet<>();
        bookings.add(booking);
        int hashBeforePersist = booking.hashCode();

        booking.setId(BOOKING_ID);

        assertThat(booking.hashCode()).isEqualTo(hashBeforePersist);
        assertThat(bookings).contains(booking);
    }

    @Test
    @DisplayName("toString carries the identifying fields and never the personal email addresses")
    void toString_whenLogged_thenOmitsEmailAddresses() {
        Booking booking = booking(BOOKING_ID, OWNER_EMAIL);

        assertThat(booking.toString())
                .contains(BOOKING_ID.toString(), OFFER_ID.toString(), BOOKED_DATE.toString())
                .doesNotContain(BOOKING_USER, OWNER_EMAIL);
    }

    @Test
    @DisplayName("equals is false against null and against a value of another type")
    void equals_whenComparedWithNullOrAValueOfAnotherType_thenNotEqual() {
        Booking booking = booking(BOOKING_ID, OWNER_EMAIL);

        assertThat(booking.equals(null)).isFalse();
        assertThat(booking.equals(BOOKING_ID.toString())).isFalse();
    }

    private static Booking booking(UUID id, String ownerEmail) {
        return Booking.builder()
                .id(id)
                .offerId(OFFER_ID)
                .bookedDate(BOOKED_DATE)
                .bookingUser(BOOKING_USER)
                .ownerEmail(ownerEmail)
                .build();
    }
}
