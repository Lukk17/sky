package com.lukk.sky.booking.domain.ports.inbound;

import com.lukk.sky.booking.domain.exception.BookingException;
import com.lukk.sky.booking.domain.exception.OfferNotFoundException;
import com.lukk.sky.booking.domain.exception.OfferServiceUnavailableException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface BookingService {

    Page<BookingView> getBookedOffersForUser(String userEmail, Pageable pageable);

    BookingView bookOffer(UUID offerID, LocalDate dateToBook, String userEmail) throws BookingException;

    void removeBooking(UUID bookingId, String userEmail);
}
