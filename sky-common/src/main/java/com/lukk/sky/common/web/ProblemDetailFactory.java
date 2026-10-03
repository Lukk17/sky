package com.lukk.sky.common.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;

public final class ProblemDetailFactory {

    public static final String RETRY_AFTER_SECONDS = "10";

    private ProblemDetailFactory() {
    }

    public static ErrorResponse notFound(Exception ex) {
        return ErrorResponse.builder(ex, HttpStatus.NOT_FOUND, ex.getMessage()).build();
    }

    public static ErrorResponse forbidden(Exception ex) {
        return ErrorResponse.builder(ex, HttpStatus.FORBIDDEN, ex.getMessage()).build();
    }

    public static ErrorResponse badRequest(Exception ex) {
        return ErrorResponse.builder(ex, HttpStatus.BAD_REQUEST, ex.getMessage()).build();
    }

    public static ErrorResponse badGateway(Exception ex) {
        return ErrorResponse.builder(ex, HttpStatus.BAD_GATEWAY, ex.getMessage()).build();
    }

    public static ErrorResponse payloadTooLarge(Exception ex) {
        return ErrorResponse.builder(ex, HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage()).build();
    }

    public static ErrorResponse unavailableWithRetry(Exception ex) {
        return ErrorResponse.builder(ex, HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage())
                .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
                .build();
    }

    public static ErrorResponse conflict(Exception ex) {
        return ErrorResponse.builder(ex, HttpStatus.CONFLICT, ex.getMessage()).build();
    }

    public static ErrorResponse conflictWithDetail(Exception ex, String detail) {
        return ErrorResponse.builder(ex, HttpStatus.CONFLICT, detail).build();
    }
}
