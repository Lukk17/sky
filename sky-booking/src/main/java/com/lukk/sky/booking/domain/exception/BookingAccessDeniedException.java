package com.lukk.sky.booking.domain.exception;

public class BookingAccessDeniedException extends BookingException {

    public BookingAccessDeniedException(String message) {
        super(message);
    }
}
