package com.lukk.sky.common.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Optional;

/**
 * Shared base for per-service @RestControllerAdvice classes.
 *
 * Concrete service handlers extend this and add @ExceptionHandler methods for their own
 * service-specific exception types (BookingException, OfferException, MessageException).
 * The validation handler is shared because every service uses the same field-error format.
 */
@Slf4j
public abstract class AbstractRestExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex) {
        StringBuilder str = new StringBuilder();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                str.append(String.format("Field '%s' %s",
                                fieldError.getField(),
                                Optional.ofNullable(fieldError.getDefaultMessage()).orElse("")))
                        .append("; ")
        );
        String errorMessage = str.toString().strip();
        log.error("Validation error in: {} with: {}", ex.getParameter(), errorMessage);
        return ErrorResponse.builder(ex, HttpStatus.BAD_REQUEST, errorMessage).build();
    }
}
