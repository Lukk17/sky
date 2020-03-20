package com.lukk.service;

import com.lukk.dto.OfferDTO;
import com.lukk.entity.Booked;
import com.lukk.entity.Offer;
import com.lukk.entity.User;
import com.lukk.repository.BookedRepository;
import com.lukk.repository.OfferRepository;
import com.lukk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class OfferServiceImpl implements OfferService {

    private final OfferRepository offerRepository;
    private final UserRepository userRepository;
    private final BookedRepository bookedRepository;

    @Override
    public List<OfferDTO> getAllOffers() {
        log.info("Pulling all offers");
        List<Offer> offers = offerRepository.findAll();
        return offers.stream()
                .map(this::convertOfferEntity_toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public OfferDTO addOffer(OfferDTO offer) {
        log.info("Saving offer from user: " + offer.getOwnerEmail());
        Offer savedOffer = offerRepository.save(convertOfferDTO_toEntity(offer));
        return convertOfferEntity_toDTO(savedOffer);
    }

    @Override
    public void deleteOffer(Long id, String userEmail) {

        offerRepository.findById(id).ifPresent(offer -> {
                    // delete offer only if its owner want to delete it
                    if (offer.getOwner().getEmail().equals(userEmail)) {
                        log.info("Deleting offer of ID: " + offer.getId());
                        offerRepository.delete(offer);
                    }
                }
        );
    }

    @Override
    public List<OfferDTO> getOwnedOffers(String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail);
        List<Offer> offers = offerRepository.findAllByOwner(owner);

        return offers.stream()
                .map(this::convertOfferEntity_toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<OfferDTO> getBookedOffers(String userEmail) {
        List<OfferDTO> offers = new ArrayList<>();
        User user = userRepository.findByEmail(userEmail);
        List<Booked> booked = bookedRepository.findAllByUser(user);

        booked.forEach(b -> {
            offers.add(convertOfferEntity_toDTO(offerRepository.findOfferByBooked(b)));
        });

        return offers;
    }

    private OfferDTO convertOfferEntity_toDTO(Offer offer) {
        return OfferDTO.builder()
                .name(offer.getName())
                .id(offer.getId())
                .city(offer.getCity())
                .country(offer.getCountry())
                .ownerEmail(offer.getOwner().getEmail())
                .description(offer.getDescription())
                .comment(offer.getComment())
                .price(offer.getPrice())
                .roomCapacity(offer.getRoomCapacity())
                .booked(offer.getBooked())
                .photoPath(offer.getPhotoPath())
                .build();
    }

    private Offer convertOfferDTO_toEntity(OfferDTO offerDTO) {
        User owner = userRepository.findByEmail(offerDTO.getOwnerEmail());

        return Offer.builder()
                .name(offerDTO.getName())
                .city(offerDTO.getCity())
                .country(offerDTO.getCountry())
                .owner(owner)
                .description(offerDTO.getDescription())
                .comment(offerDTO.getComment())
                .price(offerDTO.getPrice())
                .roomCapacity(offerDTO.getRoomCapacity())
                .booked(offerDTO.getBooked())
                .photoPath(offerDTO.getPhotoPath())
                .build();
    }
}
