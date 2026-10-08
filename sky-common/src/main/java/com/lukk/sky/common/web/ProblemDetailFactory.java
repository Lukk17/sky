package com.lukk.sky.common.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;

public final class ProblemDetailFactory {

    private static final Logger log = LoggerFactory.getLogger(ProblemDetailFactory.class);

    public static final String RETRY_AFTER_SECONDS = "10";

    private ProblemDetailFactory() {
    }

    public static ErrorResponse notFound(Exception ex) {
        log.debug("not_found: {}", safeMessage(ex));
        return ErrorResponse.builder(ex, HttpStatus.NOT_FOUND, "Resource not found.").build();
    }

    public static ErrorResponse forbidden(Exception ex) {
        log.warn("forbidden: {}", safeMessage(ex));
        return ErrorResponse.builder(ex, HttpStatus.FORBIDDEN, "Access denied.").build();
    }

    public static ErrorResponse badRequest(Exception ex) {
        log.debug("bad_request: {}", safeMessage(ex));
        return ErrorResponse.builder(ex, HttpStatus.BAD_REQUEST, "Invalid request.").build();
    }

    public static ErrorResponse badGateway(Exception ex) {
        log.warn("bad_gateway: {}", safeMessage(ex));
        return ErrorResponse.builder(ex, HttpStatus.BAD_GATEWAY, "Upstream service failed.").build();
    }

    public static ErrorResponse payloadTooLarge(Exception ex) {
        log.debug("payload_too_large: {}", safeMessage(ex));
        return ErrorResponse.builder(ex, HttpStatus.PAYLOAD_TOO_LARGE, "Request payload too large.").build();
    }

    public static ErrorResponse unavailableWithRetry(Exception ex) {
        log.warn("service_unavailable: {}", safeMessage(ex));
        return ErrorResponse.builder(ex, HttpStatus.SERVICE_UNAVAILABLE, "Service temporarily unavailable, please retry.")
                .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
                .build();
    }

    public static ErrorResponse conflict(Exception ex) {
        log.debug("conflict: {}", safeMessage(ex));
        return ErrorResponse.builder(ex, HttpStatus.CONFLICT, "Request conflicts with current state.").build();
    }

    public static ErrorResponse conflictWithDetail(Exception ex, String detail) {
        log.debug("conflict: {}", safeMessage(ex));
        return ErrorResponse.builder(ex, HttpStatus.CONFLICT, "Request conflicts with current state.").build();
    }

    private static String safeMessage(Exception ex) {
        if (ex == null || ex.getMessage() == null) {
            return "no message";
        }
        return ex.getMessage();
    }
}
