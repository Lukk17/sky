package com.lukk.sky.booking.domain.ports.service;

import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.domain.exception.BookingException;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Service interface for managing bookings.
 * This service provides operations for retrieving bookings, making a booking, and removing a booking.
 */
public interface BookingService {

    /**
     * Retrieves the offers booked by a user.
     *
     * @param userEmail The email of the user.
     * @return The list of offers booked by the user.
     */
    List<BookingDTO> getBookedOffersForUser(String userEmail);

    /**
     * Makes a booking for an offer.
     *
     * @param offerID    The ID of the offer to be booked.
     * @param dateToBook The date for which to book the offer.
     * @param userEmail  The email of the user making the booking.
     * @return The booking.
     * @throws BookingException if the booking cannot be made.
     */
    Mono<BookingDTO> bookOffer(String offerID, String dateToBook, String userEmail) throws BookingException;

    /**
     * Removes a booking.
     *
     * @param bookingId The ID of the booking to be removed.
     * @param userEmail The email of the user making the request.
     * @return A confirmation message.
     */
    String removeBooking(String bookingId, String userEmail);
}
