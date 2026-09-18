package com.lukk.sky.offer.domain.ports.outbound;

import com.lukk.sky.offer.adapters.dto.OfferDTO;

import java.util.UUID;

/**
 * Driven port the domain calls to announce an offer change.
 * The adapter owns the wire envelope and the serialisation, so the domain names the event and nothing else.
 */
public interface OfferNotificationService {

    /**
     * Announces an offer the domain has just persisted.
     *
     * @param ownerEmail the identity the notification is addressed to
     */
    void publishCreated(OfferDTO offer, String ownerEmail);

    /**
     * Announces an offer the domain has just updated.
     *
     * @param ownerEmail the identity the notification is addressed to
     */
    void publishEdited(OfferDTO offer, String ownerEmail);

    /**
     * Announces an offer the domain has just deleted.
     *
     * @param ownerEmail the identity the notification is addressed to
     */
    void publishDeleted(UUID offerId, String ownerEmail);
}
