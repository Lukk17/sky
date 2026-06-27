package com.lukk.sky.booking.domain.service;


import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.domain.exception.BookingException;
import com.lukk.sky.booking.domain.exception.BookingNotFoundException;
import com.lukk.sky.booking.domain.model.Booking;
import com.lukk.sky.booking.domain.ports.inbound.BookingService;
import com.lukk.sky.booking.domain.ports.outbound.BookingRepository;
import com.lukk.sky.booking.domain.ports.outbound.RestClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.lukk.sky.common.web.DateTimeConstants.DATE_FORMAT;

@Service
@RequiredArgsConstructor
@Slf4j
@Primary
public class BookingServicePrimary implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingPersister bookingPersister;
    private final RestClient restClient;

    @Override
    @Transactional(readOnly = true)
    public Page<BookingDTO> getBookedOffersForUser(String userEmail, Pageable pageable) {
        log.info("Pulling bookings for user: {} page={} size={}",
                userEmail, pageable.getPageNumber(), pageable.getPageSize());

        return bookingRepository.findAllByBookingUser(userEmail, pageable).map(BookingDTO::of);
    }

    @Override
    public BookingDTO bookOffer(UUID offerId, String dateToBookUnparsed, String userEmail)
            throws BookingException {
        log.info("Booking offer with ID: {} by user: {}", offerId, userEmail);

        String ownerEmail = restClient.requestOfferOwner(offerId);

        LocalDate dateToBook = LocalDate.parse(dateToBookUnparsed, DATE_FORMAT);
        List<Booking> existingBookings = getBookingsForOffer(offerId);

        checkIfBookingDateIsInFuture(dateToBook);

        Booking newBooking = createNewBooked(offerId, userEmail, dateToBook, ownerEmail);
        Booking saved = bookingPersister.saveAndPublish(newBooking, existingBookings, dateToBook);

        log.info("Offer with ID: {} booked for date: {} by user: {}",
                offerId, dateToBook.format(DATE_FORMAT), userEmail);

        return BookingDTO.of(saved);
    }

    @Override
    @Transactional
    public String removeBooking(UUID bookingId, String userEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(String.format("No booking with ID: %s found.", bookingId)));

        if (booking.getBookingUser().equals(userEmail)) {
            bookingRepository.delete(booking);
            log.info("Booking removed by user");

            return "Booking removed by user";

        } else if (booking.getOwnerEmail().equals(userEmail)) {
            bookingRepository.delete(booking);
            log.info("Booking removed by owner");

            return "Booking removed by owner";

        } else {
            throw new BookingException(String.format(
                    "User: %s can't delete booking with ID: %s because it's not booked or owned by him.",
                    userEmail, bookingId));
        }
    }

    private List<Booking> getBookingsForOffer(UUID offerId) {
        log.info("Getting bookings for offer with ID: {}", offerId);

        return bookingRepository.findAllByOfferId(offerId);
    }

    private static void checkIfBookingDateIsInFuture(LocalDate dateToBook) throws BookingException {
        LocalDate now = LocalDate.now();
        if (now.isAfter(dateToBook)) {
            throw new BookingException("You try to book offer with date in the past.");
        }
    }

    private Booking createNewBooked(UUID offerId, String bookingUser, LocalDate dateToBook, String ownerEmail) {
        return Booking.builder()
                .offerId(offerId)
                .bookedDate(dateToBook)
                .bookingUser(bookingUser)
                .ownerEmail(ownerEmail)
                .build();
    }
}
