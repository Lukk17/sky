package com.lukk.sky.booking.Assemblers;


import com.lukk.sky.booking.domain.model.Event;
import com.lukk.sky.booking.domain.model.EventType;

import java.time.LocalDateTime;

import static com.lukk.sky.booking.Assemblers.BookingAssembler.TEST_DEFAULT_BOOKED_ID;


public class EventAssembler {

    public static LocalDateTime TEST_DATE = LocalDateTime.of(2201, 6, 20,16,35,47);
    public static EventType TEST_EVENT_TYPE = EventType.BOOKED;

    public static Event getTestEvent(int sequence, String payload){
        return Event.builder()
                .bookingId(TEST_DEFAULT_BOOKED_ID)
                .sequenceNumber(sequence)
                .eventType(TEST_EVENT_TYPE)
                .payload(payload)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
