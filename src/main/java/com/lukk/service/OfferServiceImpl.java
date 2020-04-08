package com.lukk.service;

import com.lukk.dto.OfferDTO;
import com.lukk.entity.Booked;
import com.lukk.entity.Offer;
import com.lukk.entity.User;
import com.lukk.exception.OfferException;
import com.lukk.repository.OfferRepository;
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

    public final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final OfferRepository offerRepository;
    private final UserService userService;
    private final BookedService bookedService;

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
    public void deleteOffer(Long offerID, String userEmail) {

        offerRepository.findById(offerID).ifPresent(offer -> {
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
        User owner = userService.findByUserEmail(ownerEmail);
        List<Offer> offers = offerRepository.findAllByOwner(owner);

        return offers.stream()
                .map(this::convertOfferEntity_toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<OfferDTO> getBookedOffers(String userEmail) {
        List<OfferDTO> offers = new ArrayList<>();
        User user = userService.findByUserEmail(userEmail);
        List<Booked> booked = bookedService.findAllByUser(user);

        booked.forEach(b -> offers.add(convertOfferEntity_toDTO(offerRepository.findOfferByBooked(b))));

        return offers;
    }

    @Override
    public List<OfferDTO> searchOffer(String searched) {
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
        Offer offer = convertOfferDTO_toEntity(offerDTO);
        offer.setId(offerDTO.getId());

        offerRepository.save(offer);
        return convertOfferEntity_toDTO(offer);
    }

    @Override
    public void bookOffer(String offerID, String dateToBookUnparsed, String bookingUserEmail) throws OfferException {

        User bookingUser = userService.findByUserEmail(bookingUserEmail);
        Offer offer = offerRepository.findById(Long.parseLong(offerID))
                .orElseThrow(() -> new OfferException("Offer could not be found in repository."));

        LocalDate dateToBook = LocalDate.parse(dateToBookUnparsed, DATE_FORMAT);
        List<Booked> bookedList = offer.getBooked();


        checkIfAlreadyBooked(bookedList, dateToBook);
        checkIfBookingDateIsInFuture(dateToBook);

        System.out.println(bookedList);

        bookedList.add(createNewBooked(offer, bookingUser, dateToBook));
        offer.setBooked(bookedList);

        offerRepository.save(offer);
    }

    private Booked createNewBooked(Offer offer, User bookingUser, LocalDate dateToBook) {
        Booked newBook = Booked.builder()
                .offer(offer)
                .user(bookingUser)
                .bookedDate(dateToBook).build();
        System.out.println("\n\n\n\n\n\n" + newBook.getBookedDate());
        bookedService.addBooked(newBook);
        return newBook;
    }


    private OfferDTO convertOfferEntity_toDTO(Offer offer) {
        return OfferDTO.builder()
                .hotelName(offer.getName())
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
        User owner = userService.findByUserEmail(offerDTO.getOwnerEmail());

        return Offer.builder()
                .name(offerDTO.getHotelName())
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
