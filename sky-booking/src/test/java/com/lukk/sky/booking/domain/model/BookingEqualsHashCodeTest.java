package com.lukk.sky.booking.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Booking — equals and hashCode contract (Hibernate-safe, id-based)")
class BookingEqualsHashCodeTest {

    private Booking buildBooking(Long id) {
        return Booking.builder()
                .id(id)
                .offerId("offer-1")
                .bookedDate(LocalDate.of(2025, 1, 1))
                .bookingUser("user@example.com")
                .ownerEmail("owner@example.com")
                .build();
    }

    @Test
    @DisplayName("equals_whenSameInstance_thenReturnTrue")
    void equals_whenSameInstance_thenReturnTrue() {
        Booking booking = buildBooking(1L);

        assertEquals(booking, booking);
    }

    @Test
    @DisplayName("equals_whenBothHaveSameId_thenReturnTrue")
    void equals_whenBothHaveSameId_thenReturnTrue() {
        Booking a = buildBooking(1L);
        Booking b = buildBooking(1L);

        assertEquals(a, b);
    }

    @Test
    @DisplayName("equals_whenIdsDiffer_thenReturnFalse")
    void equals_whenIdsDiffer_thenReturnFalse() {
        Booking a = buildBooking(1L);
        Booking b = buildBooking(2L);

        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("equals_whenOneIdIsNull_thenReturnFalse")
    void equals_whenOneIdIsNull_thenReturnFalse() {
        Booking withId = buildBooking(1L);
        Booking withoutId = buildBooking(null);

        assertNotEquals(withId, withoutId);
        assertNotEquals(withoutId, withId);
    }

    @Test
    @DisplayName("hashCode_whenEqualBookings_thenSameHashCode")
    void hashCode_whenEqualBookings_thenSameHashCode() {
        Booking a = buildBooking(1L);
        Booking b = buildBooking(1L);

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
