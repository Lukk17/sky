package com.lukk.sky.booking.domain.ports.service;

import com.lukk.sky.booking.domain.exception.BookingException;
import com.lukk.sky.booking.domain.model.Booking;
import com.lukk.sky.booking.domain.model.EventType;
import com.lukk.sky.booking.domain.ports.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingPersister {

    private final BookingRepository bookingRepository;
    private final EventSourceService eventSourceService;

    @Transactional
    public Booking saveAndPublish(Booking booking, List<Booking> existingBookings, LocalDate dateToBook) {
        checkIfAlreadyBooked(existingBookings, dateToBook);

        log.info("Saving booking to DB with data {}", booking);
        Booking saved = bookingRepository.save(booking);

        eventSourceService.saveEvent(saved, EventType.BOOKED);

        return saved;
    }

    private static void checkIfAlreadyBooked(List<Booking> bookedList, LocalDate dateToBook) {
        if (bookedList.stream().anyMatch(b -> b.getBookedDate().isEqual(dateToBook))) {
            throw new BookingException("Offer you try to book was already booked on that date.");
        }
    }
}
