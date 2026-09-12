package com.lukk.sky.booking.adapters.outbound.persistence;

import com.lukk.sky.booking.domain.model.Event;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventSpecifications {

    static final String BOOKING_ID = "bookingId";
    static final String SEQUENCE_NUMBER = "sequenceNumber";

    public static Specification<Event> hasBookingId(UUID bookingId) {
        return (root, query, builder) -> builder.equal(root.get(BOOKING_ID), bookingId);
    }
}
