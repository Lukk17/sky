package com.lukk.sky.booking.domain.ports.outbound;

import com.lukk.sky.booking.adapters.dto.BookingDTO;

/**
 * Driven port the domain calls to announce a booking change.
 * The adapter owns the wire envelope and the serialisation, so the domain names the event and nothing else.
 */
public interface BookingNotificationService {

    /**
     * Announces a booking the domain has just persisted.
     *
     * @param userEmail the identity the notification is addressed to
     */
    void publishCreated(BookingDTO booking, String userEmail);

    /**
     * Announces a booking the domain has just removed.
     *
     * @param removalMessage the confirmation sentence the acting user is shown
     * @param userEmail      the identity the notification is addressed to
     */
    void publishRemoved(String removalMessage, String userEmail);
}
