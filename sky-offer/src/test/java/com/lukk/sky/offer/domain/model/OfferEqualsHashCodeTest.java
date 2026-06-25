package com.lukk.sky.offer.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Offer — equals and hashCode contract (Hibernate-safe, id-based)")
class OfferEqualsHashCodeTest {

    private Offer buildOffer(Long id) {
        return Offer.builder()
                .id(id)
                .hotelName("Hotel")
                .ownerEmail("owner@example.com")
                .city("Warsaw")
                .country("PL")
                .price(BigDecimal.TEN)
                .roomCapacity(2L)
                .build();
    }

    @Test
    @DisplayName("equals_whenSameInstance_thenReturnTrue")
    void equals_whenSameInstance_thenReturnTrue() {
        Offer offer = buildOffer(1L);

        assertEquals(offer, offer);
    }

    @Test
    @DisplayName("equals_whenBothHaveSameId_thenReturnTrue")
    void equals_whenBothHaveSameId_thenReturnTrue() {
        Offer a = buildOffer(1L);
        Offer b = buildOffer(1L);

        assertEquals(a, b);
    }

    @Test
    @DisplayName("equals_whenIdsDiffer_thenReturnFalse")
    void equals_whenIdsDiffer_thenReturnFalse() {
        Offer a = buildOffer(1L);
        Offer b = buildOffer(2L);

        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("equals_whenOneIdIsNull_thenReturnFalse")
    void equals_whenOneIdIsNull_thenReturnFalse() {
        Offer withId = buildOffer(1L);
        Offer withoutId = buildOffer(null);

        assertNotEquals(withId, withoutId);
        assertNotEquals(withoutId, withId);
    }

    @Test
    @DisplayName("equals_whenComparedToNull_thenReturnFalse")
    void equals_whenComparedToNull_thenReturnFalse() {
        Offer offer = buildOffer(1L);

        assertNotEquals(null, offer);
    }

    @Test
    @DisplayName("hashCode_whenEqualOffers_thenSameHashCode")
    void hashCode_whenEqualOffers_thenSameHashCode() {
        Offer a = buildOffer(1L);
        Offer b = buildOffer(1L);

        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("hashCode_whenNullId_thenConsistentWithEquals")
    void hashCode_whenNullId_thenConsistentWithEquals() {
        Offer a = buildOffer(null);
        Offer b = buildOffer(null);

        int hashA = a.hashCode();
        int hashB = b.hashCode();

        assertEquals(hashA, hashB);
        assertNotEquals(a, b);
    }
}
