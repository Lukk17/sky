package com.lukk.sky.message.domain.exception;

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

    @ExceptionHandler(MessageNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleMessageNotFound(MessageNotFoundException ex) {
        log.warn("message_not_found message={} exceptionType={}", ex.getMessage(), ex.getClass().getSimpleName());
        return ProblemDetailFactory.notFound(ex);
    }

    @ExceptionHandler(MessageAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleMessageAccessDenied(MessageAccessDeniedException ex) {
        log.warn("message_access_denied message={} exceptionType={}", ex.getMessage(), ex.getClass().getSimpleName());
        return ProblemDetailFactory.forbidden(ex);
    }

    @ExceptionHandler(MessageException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMessageExceptions(MessageException ex) {
        log.warn("message_error message={} exceptionType={}", ex.getMessage(), ex.getClass().getSimpleName());
        return ProblemDetailFactory.badRequest(ex);
    }
}
