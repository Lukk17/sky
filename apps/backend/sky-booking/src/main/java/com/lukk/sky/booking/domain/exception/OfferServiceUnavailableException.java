package com.lukk.sky.booking.domain.exception;

public class OfferServiceUnavailableException extends RuntimeException {

    public OfferServiceUnavailableException(String message) {
        super(message);
    }

    public OfferServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
