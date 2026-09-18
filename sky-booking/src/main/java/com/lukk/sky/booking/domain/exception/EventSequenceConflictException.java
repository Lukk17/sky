package com.lukk.sky.booking.domain.exception;

public class EventSequenceConflictException extends RuntimeException {

    public EventSequenceConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
