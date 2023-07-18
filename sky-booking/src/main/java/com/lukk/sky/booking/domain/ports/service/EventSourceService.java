package com.lukk.sky.booking.domain.ports.service;

import com.lukk.sky.booking.domain.model.Booking;
import com.lukk.sky.booking.domain.model.EventType;

/**
 * Service interface for handling event sources related to bookings.
 * This service provides operations for saving events.
 */
public interface EventSourceService {
    /**
     * Saves an event.
     *
     * @param booking   The booking related to the event.
     * @param eventType The type of the event.
     */
    void saveEvent(Booking booking, EventType eventType);
}
