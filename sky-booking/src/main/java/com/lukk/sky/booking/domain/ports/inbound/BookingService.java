package com.lukk.sky.booking.domain.ports.inbound;

import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.domain.exception.BookingException;
import com.lukk.sky.booking.domain.exception.OfferNotFoundException;
import com.lukk.sky.booking.domain.exception.OfferServiceUnavailableException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service interface for managing bookings.
 * This service provides operations for retrieving bookings, making a booking, and removing a booking.
 */
public interface BookingService {

    /**
     * Retrieves a paginated view of the offers booked by a user.
     *
     * @param userEmail The email of the user.
     * @param pageable  Pagination and sort parameters.
     * @return A page of offers booked by the user.
     */
    Page<BookingDTO> getBookedOffersForUser(String userEmail, Pageable pageable);

    /**
     * Makes a booking for an offer.
     *
     * @param offerID    The ID of the offer to be booked.
     * @param dateToBook The date for which to book the offer, already parsed by the inbound adapter.
     * @param userEmail  The email of the user making the booking.
     * @return The booking.
     * @throws BookingException if the request itself is wrong, for example a date in the past.
     * @throws OfferNotFoundException if the offer service holds no offer with that id.
     * @throws OfferServiceUnavailableException if the offer service could not be reached.
     */
    BookingDTO bookOffer(UUID offerID, LocalDate dateToBook, String userEmail) throws BookingException;

    /**
     * Removes a booking and announces the removal.
     *
     * @param bookingId The ID of the booking to be removed.
     * @param userEmail The email of the user making the request.
     */
    void removeBooking(UUID bookingId, String userEmail);
}
