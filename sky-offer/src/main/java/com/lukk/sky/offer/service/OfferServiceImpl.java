package com.lukk.sky.offer.service;

import com.lukk.sky.offer.dto.OfferDTO;
import com.lukk.sky.offer.entity.Booked;
import com.lukk.sky.offer.entity.Offer;
import com.lukk.sky.offer.exception.OfferException;
import com.lukk.sky.offer.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import javax.validation.ValidationException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class OfferServiceImpl implements OfferService {

    public final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final OfferRepository offerRepository;
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
        log.info("Saving offer: " + offer.toString());
        Offer savedOffer = offerRepository.save(convertNewOfferDTO_toEntity(offer));
        return convertOfferEntity_toDTO(savedOffer);
    }

    @Override
    public void deleteOffer(Long offerID, String userEmail) {

        offerRepository.findById(offerID).ifPresent(offer -> {
                    // delete offer only if its owner want to delete it
                    if (offer.getOwnerEmail().equals(userEmail)) {
                        log.info("Deleting offer of ID: " + offer.getId());
                        offerRepository.delete(offer);
                    }
                }
        );
    }

    @Override
    public List<OfferDTO> getOwnedOffers(String ownerEmail) {
        List<Offer> offers = offerRepository.findAllByOwnerEmail(ownerEmail);

        return offers.stream()
                .map(this::convertOfferEntity_toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<OfferDTO> getBookedOffers(String userEmail) {
        List<OfferDTO> offers = new ArrayList<>();
        List<Booked> booked = bookedService.findAllByUser(userEmail);

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

        Offer offer = convertEditedOfferDTO_ToEntity(offerDTO);
        offer.setId(offerDTO.getId());

        offerRepository.save(offer);
        return convertOfferEntity_toDTO(offer);
    }

    @Override
    public void bookOffer(String offerID, String dateToBookUnparsed, String bookingUserEmail) throws OfferException {

        Offer offer = offerRepository.findById(Long.parseLong(offerID))
                .orElseThrow(() -> new OfferException("Offer could not be found in repository."));

        LocalDate dateToBook = LocalDate.parse(dateToBookUnparsed, DATE_FORMAT);
        List<Booked> bookedList = offer.getBooked();


        checkIfAlreadyBooked(bookedList, dateToBook);
        checkIfBookingDateIsInFuture(dateToBook);

        bookedList.add(createNewBooked(offer, bookingUserEmail, dateToBook));
        offer.setBooked(bookedList);

        offerRepository.save(offer);
    }

    private Booked createNewBooked(Offer offer, String bookingUser, LocalDate dateToBook) {
        Booked newBook = Booked.builder()
                .offer(offer)
                .userEmail(bookingUser)
                .bookedDate(dateToBook).build();
        bookedService.addBooked(newBook);
        return newBook;
    }


    private OfferDTO convertOfferEntity_toDTO(Offer offer) {
        return OfferDTO.builder()
                .hotelName(offer.getHotelName())
                .id(offer.getId())
                .city(offer.getCity())
                .country(offer.getCountry())
                .ownerEmail(offer.getOwnerEmail())
                .description(offer.getDescription())
                .comment(offer.getComment())
                .price(offer.getPrice())
                .roomCapacity(offer.getRoomCapacity())
                .booked(offer.getBooked())
                .photoPath(offer.getPhotoPath())
                .build();
    }

    private Offer convertNewOfferDTO_toEntity(OfferDTO offerDTO) {

        return Offer.builder()
                .hotelName(offerDTO.getHotelName())
                .city(offerDTO.getCity())
                .country(offerDTO.getCountry())
                .ownerEmail(offerDTO.getOwnerEmail())
                .description(offerDTO.getDescription())
                .comment(offerDTO.getComment())
                .price(offerDTO.getPrice())
                .roomCapacity(offerDTO.getRoomCapacity())
                .booked(offerDTO.getBooked())
                .photoPath(offerDTO.getPhotoPath())
                .build();
    }

    private Offer convertEditedOfferDTO_ToEntity(OfferDTO offerDTO) {
        Offer dbOffer = offerRepository
                .findById(offerDTO.getId())
                .orElseThrow(() -> new ValidationException("Can't edit offer without its ID."));

        return createOffer_FromOnlyNotNull_OfferDTOProperties(offerDTO, dbOffer);
    }

    private Offer createOffer_FromOnlyNotNull_OfferDTOProperties(OfferDTO offerDTO, Offer dbOffer) {
        Offer.OfferBuilder builder = Offer.builder();

        builder.hotelName(Optional.ofNullable(offerDTO.getHotelName()).orElse(dbOffer.getHotelName()));
        builder.city(Optional.ofNullable(offerDTO.getCity()).orElse(dbOffer.getCity()));
        builder.country(Optional.ofNullable(offerDTO.getCountry()).orElse(dbOffer.getCountry()));
        builder.ownerEmail(Optional.ofNullable(offerDTO.getOwnerEmail()).orElse(dbOffer.getOwnerEmail()));
        builder.description(Optional.ofNullable(offerDTO.getDescription()).orElse(dbOffer.getDescription()));
        builder.comment(Optional.ofNullable(offerDTO.getComment()).orElse(dbOffer.getComment()));
        builder.price(Optional.ofNullable(offerDTO.getPrice()).orElse(dbOffer.getPrice()));
        builder.roomCapacity(Optional.ofNullable(offerDTO.getRoomCapacity()).orElse(dbOffer.getRoomCapacity()));
        builder.booked(Optional.ofNullable(offerDTO.getBooked()).orElse(dbOffer.getBooked()));
        builder.photoPath(Optional.ofNullable(offerDTO.getPhotoPath()).orElse(dbOffer.getPhotoPath()));

        return builder.build();
    }
}
