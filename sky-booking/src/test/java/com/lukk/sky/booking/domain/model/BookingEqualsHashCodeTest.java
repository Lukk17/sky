package com.lukk.sky.booking.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DisplayName("Booking — equals and hashCode contract (Hibernate-safe, id-based)")
class BookingEqualsHashCodeTest {

    private static final UUID ID_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ID_B = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OFFER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");

    private Booking buildBooking(UUID id) {
        return Booking.builder()
                .id(id)
                .offerId(OFFER_ID)
                .bookedDate(LocalDate.of(2025, 1, 1))
                .bookingUser("user@example.com")
                .ownerEmail("owner@example.com")
                .build();
    }

    @Test
    @DisplayName("equals_whenSameInstance_thenReturnTrue")
    void equals_whenSameInstance_thenReturnTrue() {
        Booking booking = buildBooking(ID_A);

        assertEquals(booking, booking);
    }

    @Test
    @DisplayName("equals_whenBothHaveSameId_thenReturnTrue")
    void equals_whenBothHaveSameId_thenReturnTrue() {
        Booking a = buildBooking(ID_A);
        Booking b = buildBooking(ID_A);

        assertEquals(a, b);
    }

    @Test
    @DisplayName("equals_whenIdsDiffer_thenReturnFalse")
    void equals_whenIdsDiffer_thenReturnFalse() {
        Booking a = buildBooking(ID_A);
        Booking b = buildBooking(ID_B);

        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("equals_whenOneIdIsNull_thenReturnFalse")
    void equals_whenOneIdIsNull_thenReturnFalse() {
        Booking withId = buildBooking(ID_A);
        Booking withoutId = buildBooking(null);

        assertNotEquals(withId, withoutId);
        assertNotEquals(withoutId, withId);
    }

    @Test
    @DisplayName("hashCode_whenEqualBookings_thenSameHashCode")
    void hashCode_whenEqualBookings_thenSameHashCode() {
        Booking a = buildBooking(ID_A);
        Booking b = buildBooking(ID_A);

        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("hashCode_whenNullId_thenConsistentWithEquals")
    void hashCode_whenNullId_thenConsistentWithEquals() {
        Booking a = buildBooking(null);
        Booking b = buildBooking(null);

        int hashA = a.hashCode();
        int hashB = b.hashCode();

        assertEquals(hashA, hashB);
        assertNotEquals(a, b);
    }
}
