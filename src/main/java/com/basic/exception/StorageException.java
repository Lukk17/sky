package com.basic.exception;

/**
 * Customized exception class for storage exception.
 * Used when uploading files.
 *
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
