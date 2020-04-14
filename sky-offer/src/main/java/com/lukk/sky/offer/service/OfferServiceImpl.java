package com.lukk.sky.offer.service;

import com.lukk.sky.offer.dto.EntityDTOConverter;
import com.lukk.sky.offer.dto.OfferDTO;
import com.lukk.sky.offer.entity.Booked;
import com.lukk.sky.offer.entity.Offer;
import com.lukk.sky.offer.exception.OfferException;
import com.lukk.sky.offer.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class OfferServiceImpl implements OfferService {

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final OfferRepository offerRepository;
    private final BookedService bookedService;
    private final EntityDTOConverter entityDTOConverter;

    private static void checkIfAlreadyBooked(List<Booked> bookedList, LocalDate dateToBook) throws OfferException {
        for (Booked booked : bookedList) {
            if (booked.getBookedDate().compareTo(dateToBook) == 0) {
                throw new OfferException("Offer you try to book was already booked on that date.");
            }
        }
    }

    private static void checkIfBookingDateIsInFuture(LocalDate dateToBook) throws OfferException {
        LocalDate now = LocalDate.now();
        if (now.compareTo(dateToBook) > 0) {
            throw new OfferException("You try to book offer with date in the past.");
        }
    }

    @Override
    public List<OfferDTO> getAllOffers() {
        log.info("Pulling all offers");
        List<Offer> offers = offerRepository.findAll();
        return offers.stream()
                .map(entityDTOConverter::convertOfferEntity_toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public OfferDTO addOffer(OfferDTO offer) throws OfferException {
        if (offer.getId() != null && offerRepository.findById(offer.getId()).isPresent()) {
            throw new OfferException("Offer with given ID already exist!");
        }

        log.info("Saving offer: " + offer.toString() + "\n  from user: " + offer.getOwnerEmail());

        Offer savedOffer = offerRepository.save(entityDTOConverter.convertNewOfferDTO_toEntity(offer));
        return entityDTOConverter.convertOfferEntity_toDTO(savedOffer);
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
                .map(entityDTOConverter::convertOfferEntity_toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<OfferDTO> getBookedOffers(String userEmail) {
        List<OfferDTO> offers = new ArrayList<>();
        List<Booked> booked = bookedService.findAllByUser(userEmail);

        booked.forEach(b -> {
            Offer offer = offerRepository.findOfferByBooked(b);
            OfferDTO offerDTO = entityDTOConverter.convertOfferEntity_toDTO(offer);

            // remove other users booking info
            List<Booked> onlyUserBooked = new ArrayList<>();
            offerDTO.getBooked().forEach(bookedByAll -> {
                if (bookedByAll.getUserEmail().equals(userEmail)) {
                    onlyUserBooked.add(bookedByAll);
                }
            });
            offerDTO.setBooked(onlyUserBooked);

            // remove duplicate offers
            if (!offers.contains(offerDTO)) {
                offers.add(offerDTO);
            }
        });

        return offers;
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
        Offer offer = entityDTOConverter.convertEditedOfferDTO_ToEntity(offerDTO);
        offer.setId(offerDTO.getId());

        offerRepository.save(offer);
        return entityDTOConverter.convertOfferEntity_toDTO(offer);
    }

    @Override
    public OfferDTO bookOffer(String offerID, String dateToBookUnparsed, String bookingUserEmail) throws OfferException {

        Offer offer = offerRepository.findById(Long.parseLong(offerID))
                .orElseThrow(() -> new OfferException("Offer could not be found in repository."));

        LocalDate dateToBook = LocalDate.parse(dateToBookUnparsed, DATE_FORMAT);
        List<Booked> bookedList = offer.getBooked();

        checkIfAlreadyBooked(bookedList, dateToBook);
        checkIfBookingDateIsInFuture(dateToBook);

        bookedList.add(createNewBooked(offer, bookingUserEmail, dateToBook));
        offer.setBooked(bookedList);
        return entityDTOConverter.convertOfferEntity_toDTO(offerRepository.save(offer));
    }

    private Booked createNewBooked(Offer offer, String bookingUser, LocalDate dateToBook) {
        Booked newBook = Booked.builder()
                .offer(offer)
                .userEmail(bookingUser)
                .bookedDate(dateToBook).build();
        Booked res = bookedService.addBooked(newBook);
        return res;
    }

}
