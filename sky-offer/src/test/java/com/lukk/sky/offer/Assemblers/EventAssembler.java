package com.lukk.sky.offer.assemblers;

import com.lukk.sky.offer.domain.model.Event;
import com.lukk.sky.offer.domain.model.EventType;

import java.time.Instant;

import static com.lukk.sky.offer.assemblers.OfferAssembler.TEST_DEFAULT_OFFER_ID;

public class EventAssembler {

    public static final Instant TEST_DATE = Instant.parse("2201-06-20T14:35:47Z");
    public static final EventType TEST_EVENT_TYPE = EventType.OFFER_CREATED;

    public static Event getTestEvent(int sequence, String payload) {
        return Event.builder()
                .offerId(TEST_DEFAULT_OFFER_ID)
                .sequenceNumber(sequence)
                .eventType(TEST_EVENT_TYPE)
                .payload(payload)
                .timestamp(Instant.now())
                .build();
    }
}
