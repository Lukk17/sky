package com.lukk.sky.offer.domain.ports.outbound;

import com.lukk.sky.offer.domain.ports.inbound.OfferView;

import java.util.UUID;

public interface OfferNotificationService {

    void publishCreated(OfferView offer, String ownerEmail);

    void publishEdited(OfferView offer, String ownerEmail);

    void publishDeleted(UUID offerId, String ownerEmail);
}
