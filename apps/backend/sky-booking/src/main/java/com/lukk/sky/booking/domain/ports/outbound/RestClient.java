package com.lukk.sky.booking.domain.ports.outbound;

import com.lukk.sky.booking.domain.exception.OfferNotFoundException;
import com.lukk.sky.booking.domain.exception.OfferServiceUnavailableException;

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
     * @throws OfferNotFoundException if the offer service holds no offer with that id.
     * @throws OfferServiceUnavailableException if the offer service could not be reached.
     */
    String requestOfferOwner(UUID offerId);
}
