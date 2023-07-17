package com.lukk.sky.offer.domain.ports.service;

import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.model.EventType;
import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.ports.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OfferServicePrimary implements OfferService {

    private final OfferRepository offerRepository;
    private final EventSourceService eventSourceService;

    @Override
    public List<OfferDTO> getAllOffers() {
        log.info("Pulling all offers");
        List<Offer> offers = offerRepository.findAll();

        return offers.stream()
                .map(OfferDTO::of)
                .collect(Collectors.toList());
    }

    @Override
    public OfferDTO addOffer(OfferDTO offerDTO) throws OfferException {

        if (offerDTO.getId() != null && offerRepository.findById(offerDTO.getId()).isPresent()) {
            throw new OfferException("Offer with given ID already exist!");
        }

        Offer savedOffer = offerRepository.save(offerDTO.toDomain());

        log.info("Saved offer with ID: {} from user: {}", savedOffer.getId(), savedOffer.getOwnerEmail());
        eventSourceService.saveEvent(savedOffer, EventType.OFFER_CREATED);

        return OfferDTO.of(savedOffer);
    }

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

    @Override
    public List<OfferDTO> getOwnedOffers(String ownerEmail) {
        log.info("Pulling offers which owner is user: {}", ownerEmail);

        List<Offer> offers = offerRepository.findAllByOwnerEmail(ownerEmail);

        return offers.stream()
                .map(OfferDTO::of)
                .collect(Collectors.toList());
    }

    @Override
    public List<OfferDTO> searchOffers(String searched) {
        log.info("Searching offers for: {}", searched);

        List<OfferDTO> offers = getAllOffers();

        return offers.stream()
                .filter(offer -> offer.getHotelName().contains(searched)
                        || offer.getOwnerEmail().contains(searched)
                        || offer.getCity().contains(searched)
                        || offer.getCountry().contains(searched)
                        || Long.toString(offer.getId()).contains(searched))
                .collect(Collectors.toList());
    }

    @Override
    public OfferDTO editOffer(OfferDTO offerDTO) {
        Offer dbOffer = offerRepository
                .findById(offerDTO.getId())
                .orElseThrow(() -> new OfferException("Offer not found."));

        dbOffer = offerRepository.save(offerDTO.mergeWithDomain(dbOffer).toDomain());

        log.info("Offer with ID: {} edited.", dbOffer.getId());

        offerDTO.setId(dbOffer.getId());
        eventSourceService.saveEvent(offerDTO.toDomain(), EventType.OFFER_UPDATED);

        return OfferDTO.of(dbOffer);
    }

    @Override
    public String findOfferOwner(String offerId){

        String ownerId = offerRepository
                .findById(Long.parseLong(offerId))
                .map(Offer::getOwnerEmail)
                .orElseThrow(() -> new OfferException(String.format("Offer with ID: %s not exist.", offerId)));

        log.info("Found owner with ID:{} of offer with ID: {}", ownerId, offerId);

        return ownerId;
    }
}
