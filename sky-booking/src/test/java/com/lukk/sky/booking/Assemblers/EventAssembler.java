package com.lukk.sky.booking.Assemblers;


import com.lukk.sky.booking.domain.model.Event;
import com.lukk.sky.booking.domain.model.EventType;

import java.time.Instant;

import static com.lukk.sky.booking.Assemblers.BookingAssembler.TEST_DEFAULT_BOOKED_ID;


public class EventAssembler {

    public static Instant TEST_DATE = Instant.parse("2201-06-20T14:35:47Z");
    public static EventType TEST_EVENT_TYPE = EventType.BOOKED;

    public static Event getTestEvent(int sequence, String payload) {
        return Event.builder()
                .bookingId(TEST_DEFAULT_BOOKED_ID)
                .sequenceNumber(sequence)
                .eventType(TEST_EVENT_TYPE)
                .payload(payload)
                .timestamp(Instant.now())
                .build();
    }
}
