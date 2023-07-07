package com.lukk.sky.booking.domain.ports.service;

import com.lukk.sky.booking.domain.model.Booking;
import com.lukk.sky.booking.domain.model.EventType;

public interface EventSourceService {
    void saveEvent(Booking booking, EventType eventType);
}
