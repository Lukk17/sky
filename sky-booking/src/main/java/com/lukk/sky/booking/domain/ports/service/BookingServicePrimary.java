package com.lukk.sky.booking.domain.ports.service;


import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.domain.exception.BookingException;
import com.lukk.sky.booking.domain.model.Booking;
import com.lukk.sky.booking.domain.model.EventType;
import com.lukk.sky.booking.domain.ports.api.RestClient;
import com.lukk.sky.booking.domain.ports.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static com.lukk.sky.common.web.DateTimeConstants.DATE_FORMAT;

/**
 * Primary implementation of the {@link BookingService}.
 * It uses {@link BookingRepository} to perform operations on the database.
 * It uses {@link EventSourceService} to manage events.
 * It uses {@link RestClient} to make requests.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
@Primary
public class BookingServicePrimary implements BookingService {

    private final BookingRepository bookingRepository;
    private final EventSourceService eventSourceService;
    private final RestClient restClient;

    /**
     * {@inheritDoc}
     * <p>Logs a message when the bookings for the user are retrieved.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<BookingDTO> getBookedOffersForUser(String userEmail, Pageable pageable) {
        log.info("Pulling bookings for user: {} page={} size={}",
                userEmail, pageable.getPageNumber(), pageable.getPageSize());

        return bookingRepository.findAllByBookingUser(userEmail, pageable).map(BookingDTO::of);
    }

    /**
     * {@inheritDoc}
     * <p>Resolves the offer owner synchronously, validates the booking request, then
     * saves the booking and the event atomically within the class-level transaction.
     *
     * @throws BookingException if the date to book is not in the future, the offer has
     *                          already been booked for that date, or the offer service
     *                          cannot be reached.
     */
    @Override
    public BookingDTO bookOffer(String offerId, String dateToBookUnparsed, String userEmail)
            throws BookingException {
        log.info("Booking offer with ID: {} by user: {}", offerId, userEmail);

        String ownerEmail = restClient.requestOfferOwner(offerId);

        LocalDate dateToBook = LocalDate.parse(dateToBookUnparsed, DATE_FORMAT);
        List<Booking> bookedList = getBookingsForOffer(offerId);

        checkIfAlreadyBooked(bookedList, dateToBook);
        checkIfBookingDateIsInFuture(dateToBook);

        Booking saved = addBooking(createNewBooked(offerId, userEmail, dateToBook, ownerEmail));

        log.info("Offer with ID: {} booked for date: {} by user: {}",
                offerId, dateToBook.format(DATE_FORMAT), userEmail);

        eventSourceService.saveEvent(saved, EventType.BOOKED);

        return BookingDTO.of(saved);
    }

    /**
     * {@inheritDoc}
     * <p>Logs a message when the booking is removed.
     * <p>Checks if the user trying to remove the booking is the user who made the booking or the owner of the booking.
     *
     * @throws BookingException if the user trying to remove the booking is neither the user who made the booking nor the owner of the booking.
     */
    @Override
    public String removeBooking(String bookingId, String userEmail) {
        long id = Long.parseLong(bookingId);
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingException(String.format("No booking with ID: %s found.", bookingId)));

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

    private List<Booking> getBookingsForOffer(String offerId) {
        log.info("Getting bookings for offer with ID: {}", offerId);

        return bookingRepository.findAllByOfferId(offerId);
    }

    private static void checkIfAlreadyBooked(List<Booking> bookedList, LocalDate dateToBook) throws BookingException {
        if (bookedList.stream().anyMatch(b -> b.getBookedDate().isEqual(dateToBook))) {
            throw new BookingException("Offer you try to book was already booked on that date.");
        }
    }

    private static void checkIfBookingDateIsInFuture(LocalDate dateToBook) throws BookingException {
        LocalDate now = LocalDate.now();
        if (now.isAfter(dateToBook)) {
            throw new BookingException("You try to book offer with date in the past.");
        }
    }

    private Booking addBooking(Booking booked) {
        log.info("Saving booking to DB with data {}", booked);

        return bookingRepository.save(booked);
    }

    private Booking createNewBooked(String offerId, String bookingUser, LocalDate dateToBook, String ownerEmail) {
        return Booking.builder()
                .offerId(offerId)
                .bookedDate(dateToBook)
                .bookingUser(bookingUser)
                .ownerEmail(ownerEmail)
                .build();
    }
}
