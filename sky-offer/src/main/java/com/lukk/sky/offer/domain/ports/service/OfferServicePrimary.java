package com.lukk.sky.offer.domain.ports.service;

import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.adapters.dto.OfferEditDTO;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.model.EventType;
import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.ports.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * This is the primary implementation of the {@link OfferService} interface.
 * It uses an {@link OfferRepository} to interact with the database.
 * It also logs operations and generates events using {@link EventSourceService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@Primary
public class OfferServicePrimary implements OfferService {

    private final OfferRepository offerRepository;
    private final EventSourceService eventSourceService;

    /**
     * {@inheritDoc}
     * <p>
     * This implementation retrieves a page of offers from the database using an {@link OfferRepository},
     * transforms them into {@link OfferDTO}s and returns a {@link Page}.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<OfferDTO> getAllOffers(Pageable pageable) {
        log.info("Pulling all offers page={} size={}", pageable.getPageNumber(), pageable.getPageSize());
        return offerRepository.findAll(pageable).map(OfferDTO::of);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation saves the given offer into the database using an {@link OfferRepository}.
     * After successful save, it logs the operation and creates an event of type {@link EventType#OFFER_CREATED}.
     *
     * @throws OfferException if an offer with the same ID already exists in the database
     */
    @Override
    public OfferDTO addOffer(OfferDTO offerDTO) throws OfferException {
        if (offerDTO.getId() != null && offerRepository.existsById(offerDTO.getId())) {
            throw new OfferException("Offer with given ID already exist!");
        }

        Offer savedOffer = offerRepository.save(offerDTO.toDomain());

        log.info("Saved offer with ID: {} from user: {}", savedOffer.getId(), savedOffer.getOwnerEmail());
        eventSourceService.saveEvent(savedOffer, EventType.OFFER_CREATED);

        return OfferDTO.of(savedOffer);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation removes the offer with the given ID from the database,
     * logs the operation and generates an event of type {@link EventType#OFFER_DELETED}.
     *
     * @throws OfferException if the offer does not exist or the user is not the owner of the offer
     */
    @Override
    public void deleteOffer(Long offerID, String userEmail) throws OfferException {
        Offer offerToDelete = offerRepository.findById(offerID)
                .orElseThrow(() -> new OfferException("Can't remove non-existing offer!"));

        if (offerToDelete.getOwnerEmail().equals(userEmail)) {
            offerRepository.delete(offerToDelete);

            log.info("Deleted offer with ID: {}", offerToDelete.getId());
            eventSourceService.saveEvent(offerToDelete, EventType.OFFER_DELETED);

        } else {
            throw new OfferException("You can't remove offer of which owner is someone else!");
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation retrieves a page of offers from the database belonging to the given owner email,
     * transforms them into {@link OfferDTO}s and returns a {@link Page}.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<OfferDTO> getOwnedOffers(String ownerEmail, Pageable pageable) {
        log.info("Pulling offers which owner is user: {} page={} size={}",
                ownerEmail, pageable.getPageNumber(), pageable.getPageSize());

        return offerRepository.findAllByOwnerEmail(ownerEmail, pageable).map(OfferDTO::of);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation searches for offers that match the given criteria,
     * transforms them into {@link OfferDTO}s and returns them as a list.
     * The search is delegated to the database via a case-insensitive LIKE query
     * across hotelName, city, country, and ownerEmail.
     */
    @Override
    @Transactional(readOnly = true)
    public List<OfferDTO> searchOffers(String searched) {
        log.info("Searching offers for: {}", searched);

        return offerRepository.searchByTerm(searched).stream()
                .map(OfferDTO::of)
                .toList();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation edits the offer in the database with the details from the given {@link OfferDTO},
     * logs the operation and creates an event of type {@link EventType#OFFER_UPDATED}.
     *
     * @throws OfferException if the offer to be edited does not exist in the database
     */
    @Override
    public OfferDTO editOffer(OfferEditDTO offerEditDTO) {
        Offer dbOffer = offerRepository
                .findById(offerEditDTO.getId())
                .orElseThrow(() -> new OfferException("Offer not found."));

        dbOffer = offerRepository.save(offerEditDTO.mergeWithDomain(dbOffer).toDomain());

        log.info("Offer with ID: {} edited.", dbOffer.getId());

        offerEditDTO.setId(dbOffer.getId());
        eventSourceService.saveEvent(offerEditDTO.toDomain(), EventType.OFFER_UPDATED);

        return OfferDTO.of(dbOffer);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation finds the owner of the offer with the given ID and returns their email.
     *
     * @throws OfferException if the offer does not exist in the database
     */
    @Override
    @Transactional(readOnly = true)
    public String findOfferOwner(String offerId) {
        String ownerEmail = offerRepository
                .findById(Long.parseLong(offerId))
                .map(Offer::getOwnerEmail)
                .orElseThrow(() -> new OfferException(String.format("Offer with ID: %s not exist.", offerId)));

        log.info("Found owner with ID:{} of offer with ID: {}", ownerEmail, offerId);

        return ownerEmail;
    }
}
