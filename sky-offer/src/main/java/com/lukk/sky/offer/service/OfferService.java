package com.lukk.sky.offer.service;

import com.lukk.sky.offer.dto.OfferDTO;
import com.lukk.sky.offer.exception.OfferException;

import java.util.List;

public interface OfferService {

    List<OfferDTO> getAllOffers();

    OfferDTO addOffer(OfferDTO offer) throws OfferException;

    void deleteOffer(Long id, String userEmail);

    List<OfferDTO> getOwnedOffers(String ownerEmail);

    List<OfferDTO> getBookedOffers(String userEmail);

    List<OfferDTO> searchOffers(String searched);

    OfferDTO editOffer(OfferDTO offer);

    OfferDTO bookOffer(String offerID, String dateToBook, String bookingUserEmail) throws OfferException;
}
