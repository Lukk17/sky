package com.lukk.sky.booking.domain.ports.outbound;

import java.util.UUID;

/**
 * A strategy interface for requesting offer owners.
 * Provides operations for requesting the owner of a specific offer.
 */
public interface RestClient {

    /**
     * Requests the owner of an offer based on the provided offer ID.
     *
     * @param offerId The ID of the offer whose owner is to be requested.
     * @return The offer owner's email address.
     */
    String requestOfferOwner(UUID offerId);
}
