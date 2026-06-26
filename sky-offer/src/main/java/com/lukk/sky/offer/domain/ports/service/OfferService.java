package com.lukk.sky.offer.domain.ports.service;

import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.adapters.dto.OfferEditDTO;
import com.lukk.sky.offer.domain.exception.OfferException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.InputStream;
import java.util.UUID;

public interface OfferService {

    Page<OfferDTO> getAllOffers(Pageable pageable);

    OfferDTO addOffer(OfferDTO offer) throws OfferException;

    void deleteOffer(UUID id, String userEmail);

    Page<OfferDTO> getOwnedOffers(String ownerEmail, Pageable pageable);

    Page<OfferDTO> searchOffers(String searched, Pageable pageable);

    OfferDTO editOffer(OfferEditDTO offerEditDTO);

    String findOfferOwner(UUID offerId);

    OfferDTO uploadPhoto(UUID offerId, String ownerEmail, InputStream content, long contentLength,
                         String validatedContentType, String filename);
}
