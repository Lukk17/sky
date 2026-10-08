package com.lukk.sky.booking.domain.exception;

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
            "A concurrent write advanced the booking event sequence. "
                    + "Re-read the booking and retry the change against its current state.";

    @ExceptionHandler(BookingNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleBookingNotFound(BookingNotFoundException ex) {
        log.warn("booking_not_found message={} exceptionType={}", ex.getMessage(), ex.getClass().getSimpleName());
        return ProblemDetailFactory.notFound(ex);
    }

    @ExceptionHandler(OfferNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleOfferNotFound(OfferNotFoundException ex) {
        log.warn("offer_not_found message={} exceptionType={}", ex.getMessage(), ex.getClass().getSimpleName());
        return ProblemDetailFactory.notFound(ex);
    }

    @ExceptionHandler(BookingAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleBookingAccessDenied(BookingAccessDeniedException ex) {
        log.warn("booking_access_denied message={} exceptionType={}", ex.getMessage(), ex.getClass().getSimpleName());
        return ProblemDetailFactory.forbidden(ex);
    }

    @ExceptionHandler(OfferServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleOfferServiceUnavailable(OfferServiceUnavailableException ex) {
        log.warn("offer_service_unavailable message={}", ex.getMessage());
        return ProblemDetailFactory.unavailableWithRetry(ex);
    }

    @ExceptionHandler(OfferServiceBadResponseException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ErrorResponse handleOfferServiceBadResponse(OfferServiceBadResponseException ex) {
        log.warn("offer_service_bad_response message={}", ex.getMessage());
        return ProblemDetailFactory.badGateway(ex);
    }

    @ExceptionHandler(BookingDateAlreadyBookedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleBookingDateAlreadyBooked(BookingDateAlreadyBookedException ex) {
        log.warn("booking_date_already_booked message={}", ex.getMessage());
        return ProblemDetailFactory.conflict(ex);
    }

    @ExceptionHandler(EventSequenceConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleEventSequenceConflict(EventSequenceConflictException ex) {
        log.warn("event_sequence_conflict message={}", ex.getMessage());
        return ProblemDetailFactory.conflictWithDetail(ex, SEQUENCE_CONFLICT_DETAIL);
    }

    @ExceptionHandler(BookingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBookingExceptions(BookingException ex) {
        log.warn("booking_error message={} exceptionType={}", ex.getMessage(), ex.getClass().getSimpleName());
        return ProblemDetailFactory.badRequest(ex);
    }
}
