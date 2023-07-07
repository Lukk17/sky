package com.lukk.sky.offer.domain.ports.service;

import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.model.Offer;

import java.util.List;
import java.util.Optional;

public interface OfferService {

    List<OfferDTO> getAllOffers();

    OfferDTO addOffer(OfferDTO offer) throws OfferException;

    void deleteOffer(Long id, String userEmail);

    List<OfferDTO> getOwnedOffers(String ownerEmail);


    List<OfferDTO> searchOffers(String searched);

    OfferDTO editOffer(OfferDTO offer);

}
