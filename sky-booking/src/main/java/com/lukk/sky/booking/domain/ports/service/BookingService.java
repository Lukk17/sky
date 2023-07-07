package com.lukk.sky.booking.domain.ports.service;

import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.domain.exception.BookingException;

import java.util.List;

public interface BookingService {

    List<BookingDTO> getBookedOffersForUser(String userEmail);

    BookingDTO bookOffer(String offerID, String dateToBook, String bookingUserEmail, String owner) throws BookingException;
}
