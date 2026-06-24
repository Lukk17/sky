package com.lukk.sky.offer.domain.ports.service;

import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.adapters.dto.OfferEditDTO;
import com.lukk.sky.offer.domain.exception.OfferException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * This interface defines methods related to offer management.
 */
public interface OfferService {

    /**
     * Retrieves a paginated view of all offers.
     *
     * @param pageable pagination and sort parameters
     * @return page of offers
     */
    Page<OfferDTO> getAllOffers(Pageable pageable);

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
     * Retrieves a paginated view of offers owned by a specific user.
     *
     * @param ownerEmail the email of the owner
     * @param pageable   pagination and sort parameters
     * @return page of offers owned by the user
     */
    Page<OfferDTO> getOwnedOffers(String ownerEmail, Pageable pageable);

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
     * @param offerEditDTO the offer to edit
     * @return the edited offer
     * @throws OfferException if the offer doesn't exist
     */

    OfferDTO editOffer(OfferEditDTO offerEditDTO);

    /**
     * Finds the owner of an offer.
     *
     * @param offerId the ID of the offer
     * @return the email of the owner
     * @throws OfferException if the offer doesn't exist
     */
    String findOfferOwner(String offerId);

    /**
     * Uploads a photo for an offer owned by {@code ownerEmail}, stores the object
     * key on the offer, and returns the updated OfferDTO with {@code photoUrl} populated.
     *
     * @param offerId      the ID of the offer
     * @param ownerEmail   email of the authenticated caller (must be offer owner)
     * @param content      raw file bytes
     * @param contentType  MIME type
     * @param filename     original filename
     * @return the updated offer DTO including the presigned photo URL
     * @throws OfferException if the offer does not exist or the caller is not the owner
     */
    OfferDTO uploadPhoto(Long offerId, String ownerEmail, byte[] content, String contentType, String filename);
}
