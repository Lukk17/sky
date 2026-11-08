package com.lukk.sky.offer.domain.exception;

public abstract class PhotoStorageException extends RuntimeException {

    protected PhotoStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
