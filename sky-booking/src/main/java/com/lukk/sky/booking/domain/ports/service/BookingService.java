package com.lukk.sky.booking.domain.ports.service;

import java.util.List;

public interface BookingService {

    List<Booked> findAllByUser(String userEmail);

    Booked addBooked(Booked booked);

    List<OfferDTO> getBookedOffers(String userEmail);

    OfferDTO bookOffer(String offerID, String dateToBook, String bookingUserEmail) throws OfferException;
}
