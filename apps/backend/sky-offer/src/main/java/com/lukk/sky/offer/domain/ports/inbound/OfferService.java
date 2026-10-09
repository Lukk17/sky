package com.lukk.sky.offer.domain.ports.inbound;

import com.lukk.sky.offer.domain.exception.OfferException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.InputStream;
import java.util.UUID;

public interface OfferService {

    Page<OfferView> getAllOffers(Pageable pageable);

    OfferView addOffer(CreateOfferCommand command) throws OfferException;

    void deleteOffer(UUID id, String userEmail);

    Page<OfferView> getOwnedOffers(String ownerEmail, Pageable pageable);

    Page<OfferView> searchOffers(String searched, Pageable pageable);

    OfferView getOfferById(UUID offerId);

    OfferView editOffer(EditOfferCommand command, String ownerEmail);

    String findOfferOwner(UUID offerId);

    OfferView uploadPhoto(UUID offerId, String ownerEmail, InputStream content, long contentLength,
                          String validatedContentType, String filename);

    void deletePhoto(UUID offerId, String ownerEmail);

    OfferView uploadGalleryPhoto(UUID offerId, String ownerEmail, InputStream content, long contentLength,
                                 String validatedContentType, String filename);

    OfferView deleteGalleryPhoto(UUID offerId, UUID photoId, String ownerEmail);

    OfferView reorderGalleryPhoto(UUID offerId, UUID photoId, int newPosition, String ownerEmail);

    OfferView setGalleryCover(UUID offerId, UUID photoId, String ownerEmail);
}
