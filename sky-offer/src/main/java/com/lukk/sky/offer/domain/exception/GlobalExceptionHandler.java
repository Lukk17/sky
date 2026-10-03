package com.lukk.sky.offer.domain.exception;

import com.lukk.sky.common.web.ProblemDetailFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String SEQUENCE_CONFLICT_DETAIL =
            "A concurrent write advanced the offer event sequence. "
                    + "Re-read the offer and retry the change against its current state.";

    @ExceptionHandler(OfferNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleOfferNotFound(OfferNotFoundException ex) {
        log.warn("offer_not_found message={} exceptionType={}", ex.getMessage(), ex.getClass().getSimpleName());
        return ProblemDetailFactory.notFound(ex);
    }

    @ExceptionHandler(OfferAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleOfferAccessDenied(OfferAccessDeniedException ex) {
        log.warn("offer_access_denied message={} exceptionType={}", ex.getMessage(), ex.getClass().getSimpleName());
        return ProblemDetailFactory.forbidden(ex);
    }

    @ExceptionHandler(EventSequenceConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleEventSequenceConflict(EventSequenceConflictException ex) {
        log.warn("event_sequence_conflict message={}", ex.getMessage());
        return ProblemDetailFactory.conflictWithDetail(ex, SEQUENCE_CONFLICT_DETAIL);
    }

    @ExceptionHandler(PhotoStorageUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handlePhotoStorageUnavailable(PhotoStorageUnavailableException ex) {
        log.warn("photo_storage_unavailable message={}", ex.getMessage());
        return ProblemDetailFactory.unavailableWithRetry(ex);
    }

    @ExceptionHandler(PhotoStorageBadResponseException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ErrorResponse handlePhotoStorageBadResponse(PhotoStorageBadResponseException ex) {
        log.warn("photo_storage_bad_response message={}", ex.getMessage());
        return ProblemDetailFactory.badGateway(ex);
    }

    @ExceptionHandler(GalleryLimitExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ErrorResponse handleGalleryLimit(GalleryLimitExceededException ex) {
        log.warn("gallery_limit_exceeded message={}", ex.getMessage());
        return ProblemDetailFactory.payloadTooLarge(ex);
    }

    @ExceptionHandler(OfferException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleOfferExceptions(OfferException ex) {
        log.warn("offer_error message={} exceptionType={}", ex.getMessage(), ex.getClass().getSimpleName());
        return ProblemDetailFactory.badRequest(ex);
    }
}
