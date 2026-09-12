package com.lukk.sky.offer.domain.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String RETRY_AFTER_SECONDS = "10";

    private static final String SEQUENCE_CONFLICT_DETAIL =
            "A concurrent write advanced the offer event sequence. "
                    + "Re-read the offer and retry the change against its current state.";

    @ExceptionHandler(OfferNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleOfferNotFound(OfferNotFoundException ex) {
        log.warn("offer_not_found message={} exceptionType={}", ex.getMessage(), ex.getClass().getSimpleName());
        return ErrorResponse.builder(ex, HttpStatus.NOT_FOUND, ex.getMessage()).build();
    }

    @ExceptionHandler(OfferAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleOfferAccessDenied(OfferAccessDeniedException ex) {
        log.warn("offer_access_denied message={} exceptionType={}", ex.getMessage(), ex.getClass().getSimpleName());
        return ErrorResponse.builder(ex, HttpStatus.FORBIDDEN, ex.getMessage()).build();
    }

    @ExceptionHandler(EventSequenceConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleEventSequenceConflict(EventSequenceConflictException ex) {
        log.warn("event_sequence_conflict message={}", ex.getMessage());
        return ErrorResponse.builder(ex, HttpStatus.CONFLICT, SEQUENCE_CONFLICT_DETAIL).build();
    }

    @ExceptionHandler(PhotoStorageUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handlePhotoStorageUnavailable(PhotoStorageUnavailableException ex) {
        log.warn("photo_storage_unavailable message={}", ex.getMessage());

        return ErrorResponse.builder(ex, HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage())
                .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
                .build();
    }

    @ExceptionHandler(PhotoStorageBadResponseException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ErrorResponse handlePhotoStorageBadResponse(PhotoStorageBadResponseException ex) {
        log.warn("photo_storage_bad_response message={}", ex.getMessage());

        return ErrorResponse.builder(ex, HttpStatus.BAD_GATEWAY, ex.getMessage()).build();
    }

    @ExceptionHandler(OfferException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleOfferExceptions(OfferException ex) {
        log.warn("offer_error message={} exceptionType={}", ex.getMessage(), ex.getClass().getSimpleName());
        return ErrorResponse.builder(ex, HttpStatus.BAD_REQUEST, ex.getMessage()).build();
    }
}
