package com.lukk.sky.common.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns every framework-level web exception into an RFC 9457 problem response, and enriches a Bean Validation
 * failure with a {@code field-errors} property. Domain exceptions stay in each service's own advice.
 */
@RestControllerAdvice
@Order(0)
@Slf4j
public class SkyRestExceptionHandler extends ResponseEntityExceptionHandler {

    static final String FIELD_ERRORS_PROPERTY = "field-errors";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage())
        );

        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        body.setProperty(FIELD_ERRORS_PROPERTY, fieldErrors);

        log.warn("validation_error parameter={} fieldErrors={}", ex.getParameter(), fieldErrors);

        return ResponseEntity.badRequest().body(body);
    }
}
