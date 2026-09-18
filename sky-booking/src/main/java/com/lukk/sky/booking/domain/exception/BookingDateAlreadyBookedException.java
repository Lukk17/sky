package com.lukk.sky.booking.domain.exception;

public class BookingDateAlreadyBookedException extends RuntimeException {

    public BookingDateAlreadyBookedException(String message) {
        super(message);
    }
}
