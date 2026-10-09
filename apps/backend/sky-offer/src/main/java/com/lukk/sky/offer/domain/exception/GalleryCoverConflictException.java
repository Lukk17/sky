package com.lukk.sky.offer.domain.exception;

public class GalleryCoverConflictException extends EventSequenceConflictException {

    public GalleryCoverConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
