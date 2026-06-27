package com.lukk.sky.offer.domain.service;

import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.adapters.dto.OfferEditDTO;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.exception.OfferNotFoundException;
import com.lukk.sky.offer.domain.model.EventType;
import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.ports.inbound.OfferService;
import com.lukk.sky.offer.domain.ports.outbound.OfferRepository;
import com.lukk.sky.offer.domain.ports.outbound.PhotoStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@Primary
public class OfferServicePrimary implements OfferService {

    private final OfferRepository offerRepository;
    private final EventSourceService eventSourceService;
    private final PhotoStorage photoStorage;

    @Override
    @Transactional(readOnly = true)
    public Page<OfferDTO> getAllOffers(Pageable pageable) {
        log.info("Pulling all offers page={} size={}", pageable.getPageNumber(), pageable.getPageSize());

        return offerRepository.findAll(pageable).map(offer -> withPresignedUrl(OfferDTO.of(offer)));
    }

    @Override
    public OfferDTO addOffer(OfferDTO offerDTO) throws OfferException {
        if (offerDTO.getId() != null && offerRepository.existsById(offerDTO.getId())) {
            throw new OfferException("Offer with given ID already exist!");
        }

        Offer savedOffer = offerRepository.save(offerDTO.toDomain());

        log.info("Saved offer with ID: {} from user: {}", savedOffer.getId(), savedOffer.getOwnerEmail());
        eventSourceService.saveEvent(savedOffer, EventType.OFFER_CREATED);

        return withPresignedUrl(OfferDTO.of(savedOffer));
    }

    @Override
    public void deleteOffer(UUID offerID, String userEmail) throws OfferException {
        Offer offerToDelete = offerRepository.findById(offerID)
                .orElseThrow(() -> new OfferNotFoundException("Can't remove non-existing offer!"));

        if (offerToDelete.getOwnerEmail().equals(userEmail)) {
            offerRepository.delete(offerToDelete);

            log.info("Deleted offer with ID: {}", offerToDelete.getId());
            eventSourceService.saveEvent(offerToDelete, EventType.OFFER_DELETED);

        } else {
            throw new OfferException("You can't remove offer of which owner is someone else!");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OfferDTO> getOwnedOffers(String ownerEmail, Pageable pageable) {
        log.info("Pulling offers which owner is user: {} page={} size={}",
                ownerEmail, pageable.getPageNumber(), pageable.getPageSize());

        return offerRepository.findAllByOwnerEmail(ownerEmail, pageable)
                .map(offer -> withPresignedUrl(OfferDTO.of(offer)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OfferDTO> searchOffers(String searched, Pageable pageable) {
        log.info("Searching offers for: {}", searched);

        return offerRepository.searchByTerm(searched, pageable)
                .map(offer -> withPresignedUrl(OfferDTO.of(offer)));
    }

    @Override
    public OfferDTO editOffer(OfferEditDTO offerEditDTO) {
        Offer dbOffer = offerRepository
                .findById(offerEditDTO.getId())
                .orElseThrow(() -> new OfferNotFoundException("Offer not found."));

        Offer savedOffer = offerRepository.save(offerEditDTO.mergeWithDomain(dbOffer).toDomain());

        log.info("Offer with ID: {} edited.", savedOffer.getId());
        eventSourceService.saveEvent(savedOffer, EventType.OFFER_UPDATED);

        return withPresignedUrl(OfferDTO.of(savedOffer));
    }

    @Override
    @Transactional(readOnly = true)
    public String findOfferOwner(UUID offerId) {
        String ownerEmail = offerRepository
                .findById(offerId)
                .map(Offer::getOwnerEmail)
                .orElseThrow(() -> new OfferNotFoundException(String.format("Offer with ID: %s not exist.", offerId)));

        log.info("Found owner with ID:{} of offer with ID: {}", ownerEmail, offerId);

        return ownerEmail;
    }

    @Override
    public OfferDTO uploadPhoto(UUID offerId, String ownerEmail, InputStream content, long contentLength,
                                String validatedContentType, String filename) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new OfferNotFoundException(String.format("Offer with ID: %s not exist.", offerId)));

        if (!offer.getOwnerEmail().equals(ownerEmail)) {
            throw new OfferException("You can only upload photos for your own offers.");
        }

        String key = photoStorage.upload(content, contentLength, validatedContentType, filename);
        offer.setPhotoPath(key);
        Offer saved = offerRepository.save(offer);

        log.info("Photo uploaded for offer ID: {} key={}", offerId, key);

        return withPresignedUrl(OfferDTO.of(saved));
    }

    private OfferDTO withPresignedUrl(OfferDTO dto) {
        String url = photoStorage.presignedUrl(dto.getPhotoPath());
        dto.setPhotoUrl(url);

        return dto;
    }
}
