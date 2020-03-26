package com.lukk.service;

import com.lukk.dto.OfferDTO;

import java.util.List;

public interface OfferService {

    List<OfferDTO> getAllOffers();

    OfferDTO addOffer(OfferDTO offer);

    void deleteOffer(Long id, String userEmail);

    List<OfferDTO> getOwnedOffers(String ownerEmail);

    List<OfferDTO> getBookedOffers(String userEmail);

    List<OfferDTO> searchOffer(String searched);

    OfferDTO editOffer(OfferDTO offer);
}
