package com.lukk.sky.booking.domain.ports.service;


import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.model.Booked;
import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.ports.repository.BookedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.lukk.sky.offer.config.Constants.DATE_FORMAT;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingServicePrimary implements BookingService {

    private final BookedRepository bookedRepository;
    private final OfferService offerService;

    @Override
    public List<Booked> findAllByUser(String userEmail) {
        return bookedRepository.findAllByUserEmail(userEmail);
    }

    @Override
    public Booked addBooked(Booked booked) {
        return bookedRepository.save(booked);
    }

    @Override
    public List<OfferDTO> getBookedOffers(String userEmail) {
        Set<OfferDTO> offers = new HashSet<>();
        List<Booked> booked = findAllByUser(userEmail);

        booked.forEach(b -> {
            Offer offer = b.getOffer();
            OfferDTO offerDTO = OfferDTO.of(offer);
            offerDTO.setBooked(removeOtherUsersBookingInfo(userEmail, offerDTO));

            offers.add(OfferDTO.of(b.getOffer()));

        });

        return new ArrayList<>(offers);
    }

    private static List<Booked> removeOtherUsersBookingInfo(String userEmail, OfferDTO offerDTO) {
        List<Booked> onlyUserBooked = new ArrayList<>();
        offerDTO.getBooked().forEach(bookedByAll -> {
            if (bookedByAll.getUserEmail().equals(userEmail)) {
                onlyUserBooked.add(bookedByAll);
            }
        });
        return onlyUserBooked;
    }

    @Override
    public OfferDTO bookOffer(String offerID, String dateToBookUnparsed, String bookingUserEmail) throws OfferException {
        Offer offer = offerService.getOffer(Long.parseLong(offerID))
                .orElseThrow(() -> new OfferException("Offer could not be found in repository."));

        LocalDate dateToBook = LocalDate.parse(dateToBookUnparsed, DATE_FORMAT);
        List<Booked> bookedList = offer.getBooked();

        checkIfAlreadyBooked(bookedList, dateToBook);
        checkIfBookingDateIsInFuture(dateToBook);

        Booked newBook = addBooked(createNewBooked(offer, bookingUserEmail, dateToBook));

        bookedList.add(newBook);
        offer.setBooked(bookedList);

        return offerService.editOffer(OfferDTO.of(offer));
    }

    private static void checkIfAlreadyBooked(List<Booked> bookedList, LocalDate dateToBook) throws OfferException {
        for (Booked booked : bookedList) {
            if (booked.getBookedDate().isEqual(dateToBook)) {
                throw new OfferException("Offer you try to book was already booked on that date.");
            }
        }
    }

    private static void checkIfBookingDateIsInFuture(LocalDate dateToBook) throws OfferException {
        LocalDate now = LocalDate.now();
        if (now.isAfter(dateToBook)) {
            throw new OfferException("You try to book offer with date in the past.");
        }
    }

    private Booked createNewBooked(Offer offer, String bookingUser, LocalDate dateToBook) {
        return Booked.builder()
                .offer(offer)
                .userEmail(bookingUser)
                .bookedDate(dateToBook).build();
    }
}
