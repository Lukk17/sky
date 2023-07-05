package com.lukk.sky.offer.domain.ports.service;

import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.model.Booked;

import java.util.List;

public interface BookingService {

    List<Booked> findAllByUser(String userEmail);

    Booked addBooked(Booked booked);

    List<OfferDTO> getBookedOffers(String userEmail);

    OfferDTO bookOffer(String offerID, String dateToBook, String bookingUserEmail) throws OfferException;
}
