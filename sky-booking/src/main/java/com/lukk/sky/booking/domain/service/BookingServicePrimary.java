package com.lukk.sky.booking.domain.service;

import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.domain.exception.BookingAccessDeniedException;
import com.lukk.sky.booking.domain.exception.BookingException;
import com.lukk.sky.booking.domain.exception.BookingNotFoundException;
import com.lukk.sky.booking.domain.model.Booking;
import com.lukk.sky.booking.domain.ports.inbound.BookingService;
import com.lukk.sky.booking.domain.ports.outbound.BookingNotificationService;
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
    private final BookingNotificationService bookingNotificationService;

    @Override
    @Transactional(readOnly = true)
    public Page<BookingDTO> getBookedOffersForUser(String userEmail, Pageable pageable) {
        log.info("Pulling bookings for user: {} page={} size={}",
                userEmail, pageable.getPageNumber(), pageable.getPageSize());

        return bookingRepository.findAllByBookingUser(userEmail, pageable).map(BookingDTO::of);
    }

    @Override
    public BookingDTO bookOffer(UUID offerId, LocalDate dateToBook, String userEmail)
            throws BookingException {
        log.info("Booking offer with ID: {} by user: {}", offerId, userEmail);

        String ownerEmail = restClient.requestOfferOwner(offerId);

        List<Booking> existingBookings = getBookingsForOffer(offerId);

        checkIfBookingDateIsInFuture(dateToBook);

        Booking newBooking = createNewBooked(offerId, userEmail, dateToBook, ownerEmail);
        Booking saved = bookingPersister.saveAndPublish(newBooking, existingBookings, dateToBook);

        log.info("Offer with ID: {} booked for date: {} by user: {}",
                offerId, dateToBook.format(DATE_FORMAT), userEmail);

        BookingDTO bookingDTO = BookingDTO.of(saved);
        bookingNotificationService.publishCreated(bookingDTO, userEmail);

        return bookingDTO;
    }

    @Override
    @Transactional
    public void removeBooking(UUID bookingId, String userEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(String.format("No booking with ID: %s found.", bookingId)));

        if (booking.getBookingUser().equals(userEmail)) {
            bookingRepository.delete(booking);
            log.info("Booking removed by user");

            bookingNotificationService.publishRemoved("Booking removed by user", userEmail);

        } else if (booking.getOwnerEmail().equals(userEmail)) {
            bookingRepository.delete(booking);
            log.info("Booking removed by owner");

            bookingNotificationService.publishRemoved("Booking removed by owner", userEmail);

        } else {
            throw new BookingAccessDeniedException(
                    "You neither booked this offer nor own it, so you cannot cancel this booking.");
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
