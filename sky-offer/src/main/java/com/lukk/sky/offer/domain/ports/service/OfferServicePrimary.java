package com.lukk.sky.offer.domain.ports.service;

import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.ports.repository.OfferRepository;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OfferServicePrimary implements OfferService {

    private final OfferRepository offerRepository;

    @Override
    public Optional<Offer> getOffer(Long offerId) {
        return offerRepository.findById(offerId);
    }

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

        log.info("Saving offer: " + offerDTO + "\n  from user: " + offerDTO.getOwnerEmail());

        Offer savedOffer = offerRepository.save(offerDTO.toDomain());
        return OfferDTO.of(savedOffer);
    }

    @Override
    public void deleteOffer(Long offerID, String userEmail) throws OfferException {

        Offer offer = offerRepository.findById(offerID).orElseThrow(() -> new OfferException("Can't remove non-existing offer!"));
        if (offer.getOwnerEmail().equals(userEmail)) {
            log.info("Deleting offer of ID: " + offer.getId());
            offerRepository.delete(offer);

        } else {
            throw new OfferException("You can't remove offer of which owner is someone else!");
        }
    }

    @Override
    public List<OfferDTO> getOwnedOffers(String ownerEmail) {
        List<Offer> offers = offerRepository.findAllByOwnerEmail(ownerEmail);

        return offers.stream()
                .map(OfferDTO::of)
                .collect(Collectors.toList());
    }

    @Override
    public List<OfferDTO> searchOffers(String searched) {
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
                .orElseThrow(() -> new ValidationException("Can't edit offer without its ID."));

        offerDTO = offerDTO.mergeWithDomain(dbOffer);
        offerRepository.save(offerDTO.toDomain());

        return offerDTO;
    }
}
