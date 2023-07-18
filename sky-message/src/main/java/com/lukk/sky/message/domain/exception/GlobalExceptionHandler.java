package com.lukk.sky.message.domain.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;

import java.util.Optional;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(MessageException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMessageExceptions(MessageException ex) {
        log.error("{} Exception class: {}", ex.getMessage(), ex.getClass().getSimpleName());

        return ErrorResponse.builder(ex, HttpStatus.BAD_REQUEST, ex.getMessage()).build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex) {
        StringBuffer str = new StringBuffer();

        ex.getBindingResult().getFieldErrors()
                .forEach(objectError -> {
                    str.append(String.format("Field '%s' %s", objectError.getField(),
                                    Optional.ofNullable(objectError.getDefaultMessage()).orElse("")))
                            .append("; ");
                });
        String errorMessage = str.toString().strip();
        log.error("Error in: {} with: {}", ex.getParameter(), errorMessage);
        return ErrorResponse.builder(ex, HttpStatus.BAD_REQUEST, errorMessage).build();
    }
}
