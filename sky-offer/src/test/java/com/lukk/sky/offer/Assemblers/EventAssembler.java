package com.lukk.sky.offer.Assemblers;

import com.lukk.sky.offer.domain.model.Event;
import com.lukk.sky.offer.domain.model.EventType;

import java.time.LocalDateTime;

import static com.lukk.sky.offer.Assemblers.OfferAssembler.TEST_DEFAULT_OFFER_ID;

public class EventAssembler {

    public static LocalDateTime TEST_DATE = LocalDateTime.of(2201, 6, 20, 16, 35, 47);
    public static EventType TEST_EVENT_TYPE = EventType.OFFER_CREATED;

    public static Event getTestEvent(int sequence, String payload) {
        return Event.builder()
                .offerId(TEST_DEFAULT_OFFER_ID)
                .sequenceNumber(sequence)
                .eventType(TEST_EVENT_TYPE)
                .payload(payload)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
