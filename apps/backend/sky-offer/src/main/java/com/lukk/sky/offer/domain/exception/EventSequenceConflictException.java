package com.lukk.sky.offer.domain.exception;

public class EventSequenceConflictException extends RuntimeException {

    public EventSequenceConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
