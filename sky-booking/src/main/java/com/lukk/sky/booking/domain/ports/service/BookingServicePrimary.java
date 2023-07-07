package com.lukk.sky.booking.domain.ports.service;


import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.domain.exception.BookingException;
import com.lukk.sky.booking.domain.model.Booking;
import com.lukk.sky.booking.domain.model.EventType;
import com.lukk.sky.booking.domain.ports.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static com.lukk.sky.booking.config.Constants.DATE_FORMAT;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BookingServicePrimary implements BookingService {

    private final BookingRepository bookingRepository;
    private final EventSourceService eventSourceService;

    @Override
    public List<BookingDTO> getBookedOffersForUser(String userEmail) {
        log.info("Pulling bookings for user: {}", userEmail);

        return bookingRepository.findAllByBookingUser(userEmail).stream()
                .map(BookingDTO::of)
                .toList();
    }

    @Override
    public BookingDTO bookOffer(String offerId, String dateToBookUnparsed, String bookingUserEmail, String ownerId)
            throws BookingException {

        LocalDate dateToBook = LocalDate.parse(dateToBookUnparsed, DATE_FORMAT);
        List<Booking> bookedList = getBookingsForOffer(offerId);

        checkIfAlreadyBooked(bookedList, dateToBook);
        checkIfBookingDateIsInFuture(dateToBook);

        Booking newBook = bookingRepository.save(
                addBooking(
                        createNewBooked(offerId, bookingUserEmail, dateToBook, ownerId))
        );

        log.info("Offer with ID: {} booked for date: {} by user: {}",
                offerId, dateToBook.format(DATE_FORMAT), bookingUserEmail);

        eventSourceService.saveEvent(newBook, EventType.BOOKED);
        return BookingDTO.of(newBook);
    }

    private List<Booking> getBookingsForOffer(String offerId) {

        return bookingRepository.findAllByOfferId(offerId);
    }

    private static void checkIfAlreadyBooked(List<Booking> bookedList, LocalDate dateToBook) throws BookingException {
        for (Booking booked : bookedList) {
            if (booked.getBookedDate().isEqual(dateToBook)) {
                throw new BookingException("Offer you try to book was already booked on that date.");
            }
        }
    }

    private static void checkIfBookingDateIsInFuture(LocalDate dateToBook) throws BookingException {
        LocalDate now = LocalDate.now();
        if (now.isAfter(dateToBook)) {
            throw new BookingException("You try to book offer with date in the past.");
        }
    }

    private Booking addBooking(Booking booked) {
        return bookingRepository.save(booked);
    }

    private Booking createNewBooked(String offerId, String bookingUser, LocalDate dateToBook, String ownerId) {
        return Booking.builder()
                .offerId(offerId)
                .bookedDate(dateToBook)
                .bookingUser(bookingUser)
                .owner(ownerId)
                .build();
    }
}
