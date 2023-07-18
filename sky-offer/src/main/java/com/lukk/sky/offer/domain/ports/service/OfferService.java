package com.lukk.sky.offer.domain.ports.service;

import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.domain.exception.OfferException;

import java.util.List;

/**
 * This interface defines methods related to offer management.
 */
public interface OfferService {

    /**
     * Retrieves all offers.
     *
     * @return list of all offers
     */
    List<OfferDTO> getAllOffers();

    /**
     * Adds a new offer.
     *
     * @param offer the offer to add
     * @return the added offer
     * @throws OfferException if an offer with the same ID already exists
     */
    OfferDTO addOffer(OfferDTO offer) throws OfferException;

    /**
     * Deletes an offer.
     *
     * @param id        the ID of the offer to delete
     * @param userEmail the email of the user attempting to delete the offer
     * @throws OfferException if the offer doesn't exist or the user isn't the owner
     */
    void deleteOffer(Long id, String userEmail);

    /**
     * Retrieves offers owned by a specific user.
     *
     * @param ownerEmail the email of the owner
     * @return list of offers owned by the user
     */
    List<OfferDTO> getOwnedOffers(String ownerEmail);

    /**
     * Searches for offers that match the provided query.
     *
     * @param searched the search query
     * @return list of matching offers
     */
    List<OfferDTO> searchOffers(String searched);

    /**
     * Edits an existing offer.
     *
     * @param offer the offer to edit
     * @return the edited offer
     * @throws OfferException if the offer doesn't exist
     */
    OfferDTO editOffer(OfferDTO offer);

    /**
     * Finds the owner of an offer.
     *
     * @param offerId the ID of the offer
     * @return the email of the owner
     * @throws OfferException if the offer doesn't exist
     */
    String findOfferOwner(String offerId);
}
