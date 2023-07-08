package com.lukk.sky.offer.domain.ports.service;

import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.domain.exception.OfferException;

import java.util.List;

public interface OfferService {

    List<OfferDTO> getAllOffers();

    OfferDTO addOffer(OfferDTO offer) throws OfferException;

    void deleteOffer(Long id, String userEmail);

    List<OfferDTO> getOwnedOffers(String ownerEmail);


    List<OfferDTO> searchOffers(String searched);

    OfferDTO editOffer(OfferDTO offer);

    String findOfferOwner(String offerId);
}
