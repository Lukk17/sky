package com.lukk.sky.offer.adapters.outbound.persistence;

import com.lukk.sky.offer.domain.model.Event;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventSpecifications {

    static final String OFFER_ID = "offerId";
    static final String SEQUENCE_NUMBER = "sequenceNumber";

    public static Specification<Event> hasOfferId(UUID offerId) {
        return (root, query, builder) -> builder.equal(root.get(OFFER_ID), offerId);
    }
}
