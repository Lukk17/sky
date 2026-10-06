package com.lukk.sky.offer.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("JPA entity equality contract: identity is the assigned id, and the hash never moves")
class EntityEqualityContractTest {

    @Test
    @DisplayName("Two unsaved offers are never equal, because neither has been given an identity yet")
    void equals_whenBothOffersAreUnsaved_thenTheyAreNotEqual() {
        assertNotEquals(unsavedOffer(), unsavedOffer());
    }

    @Test
    @DisplayName("Two offers with the same id are equal even when their business fields differ")
    void equals_whenIdsMatch_thenOffersAreEqualRegardlessOfOtherFields() {
        UUID id = UUID.randomUUID();
        Offer offer = unsavedOffer();
        offer.setId(id);
        Offer sameOffer = unsavedOffer();
        sameOffer.setId(id);
        sameOffer.setHotelName("A completely different name");

        assertEquals(offer, sameOffer);
    }

    @Test
    @DisplayName("Two offers with different ids are not equal even when every business field matches")
    void equals_whenIdsDiffer_thenOffersAreNotEqual() {
        Offer offer = unsavedOffer();
        offer.setId(UUID.randomUUID());
        Offer otherOffer = unsavedOffer();
        otherOffer.setId(UUID.randomUUID());

        assertNotEquals(offer, otherOffer);
    }

    @Test
    @DisplayName("A saved offer is never equal to null, so a missing row never matches a real one")
    void equals_whenOfferIsComparedWithNull_thenTheyAreNotEqual() {
        Offer offer = unsavedOffer();
        offer.setId(UUID.randomUUID());

        assertNotEquals(offer, null);
    }

    @Test
    @DisplayName("An offer and an event that share one id are still not equal, because identity includes the entity type")
    void equals_whenAnOfferAndAnEventShareAnId_thenNeitherIsEqualToTheOther() {
        UUID sharedId = UUID.randomUUID();
        Offer offer = unsavedOffer();
        offer.setId(sharedId);
        Event event = unsavedEvent();
        event.setId(sharedId);

        assertNotEquals(offer, event);
        assertNotEquals(event, offer);
    }

    @Test
    @DisplayName("An offer stays findable in a HashSet after persistence assigns its id")
    void hashCode_whenIdIsAssignedAfterTheOfferJoinedASet_thenTheOfferIsStillFound() {
        Offer offer = unsavedOffer();
        Set<Offer> offers = new HashSet<>();
        offers.add(offer);

        offer.setId(UUID.randomUUID());

        assertTrue(offers.contains(offer));
    }

    @Test
    @DisplayName("Two unsaved events are never equal, because neither has been given an identity yet")
    void equals_whenBothEventsAreUnsaved_thenTheyAreNotEqual() {
        assertNotEquals(unsavedEvent(), unsavedEvent());
    }

    @Test
    @DisplayName("An unsaved event is found in a list that holds it, while an identical twin in the same list is not it")
    void equals_whenAnUnsavedEventIsLookedUpInAList_thenOnlyThatInstanceMatches() {
        Event event = unsavedEvent();
        Event twin = unsavedEvent();
        List<Event> events = List.of(twin, event);

        assertTrue(events.contains(event));
        assertEquals(1, events.indexOf(event));
    }

    @Test
    @DisplayName("Two events with the same id are equal even when their payloads differ")
    void equals_whenEventIdsMatch_thenEventsAreEqualRegardlessOfOtherFields() {
        UUID id = UUID.randomUUID();
        Event event = unsavedEvent();
        event.setId(id);
        Event sameEvent = unsavedEvent();
        sameEvent.setId(id);
        sameEvent.setPayload("{\"hotelName\":\"A completely different payload\"}");

        assertEquals(event, sameEvent);
    }

    @Test
    @DisplayName("Two events with different ids are not equal even when every business field matches")
    void equals_whenEventIdsDiffer_thenEventsAreNotEqual() {
        Event event = unsavedEvent();
        Event otherEvent = unsavedEvent();
        otherEvent.setOfferId(event.getOfferId());
        otherEvent.setTimestamp(event.getTimestamp());
        event.setId(UUID.randomUUID());
        otherEvent.setId(UUID.randomUUID());

        assertNotEquals(event, otherEvent);
    }

    @Test
    @DisplayName("A saved event is never equal to null, so a missing row never matches a real one")
    void equals_whenEventIsComparedWithNull_thenTheyAreNotEqual() {
        Event event = unsavedEvent();
        event.setId(UUID.randomUUID());

        assertNotEquals(event, null);
    }

    @Test
    @DisplayName("An event stays findable in a HashSet after persistence assigns its id")
    void hashCode_whenIdIsAssignedAfterTheEventJoinedASet_thenTheEventIsStillFound() {
        Event event = unsavedEvent();
        Set<Event> events = new HashSet<>();
        events.add(event);

        event.setId(UUID.randomUUID());

        assertTrue(events.contains(event));
    }

    private static Offer unsavedOffer() {
        return Offer.builder()
                .hotelName("Grand Hotel")
                .city("Warsaw")
                .country("Poland")
                .ownerEmail("owner@offer.com")
                .price(BigDecimal.valueOf(100))
                .roomCapacity(2L)
                .build();
    }

    private static Event unsavedEvent() {
        return Event.builder()
                .offerId(UUID.randomUUID())
                .sequenceNumber(1)
                .eventType(EventType.OFFER_CREATED)
                .payload("{}")
                .timestamp(Instant.now())
                .build();
    }
}
