package com.lukk.sky.common.web;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Turns a Spring Data failure caused by a caller-supplied value into a 400 problem detail.
 * Without it an unknown {@code sort} property, or a value the database cannot store, escapes as a 500.
 */
@RestControllerAdvice
@Order(0)
@Slf4j
public class SpringDataExceptionHandler {

    static final String UNNAMEABLE_PROPERTY_DETAIL =
            "The requested sort property is not a sortable property of this resource.";

    static final String UNENCODABLE_CHARACTER_DETAIL =
            "A text field contains a character the database cannot store, such as the NUL character U+0000. "
                    + "Remove it and send the request again.";

    static final String VALUE_TOO_LONG_DETAIL =
            "A text field is longer than the database column that stores it allows.";

    static final String NUMERIC_OUT_OF_RANGE_DETAIL =
            "A numeric field is outside the range the database column that stores it allows.";

    private static final String CHARACTER_NOT_IN_REPERTOIRE = "22021";
    private static final String STRING_DATA_RIGHT_TRUNCATION = "22001";
    private static final String NUMERIC_VALUE_OUT_OF_RANGE = "22003";

    private static final Map<String, String> DETAIL_BY_SQL_STATE = Map.of(
            CHARACTER_NOT_IN_REPERTOIRE, UNENCODABLE_CHARACTER_DETAIL,
            STRING_DATA_RIGHT_TRUNCATION, VALUE_TOO_LONG_DETAIL,
            NUMERIC_VALUE_OUT_OF_RANGE, NUMERIC_OUT_OF_RANGE_DETAIL);

    private static final Pattern SAFE_TO_ECHO = Pattern.compile("[A-Za-z0-9_.]{1,64}");

    @ExceptionHandler(PropertyReferenceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnknownSortProperty(PropertyReferenceException ex) {
        String propertyName = ex.getPropertyName();

        log.warn("unknown_sort_property property={}", propertyName);

        return ErrorResponse.builder(ex, HttpStatus.BAD_REQUEST, detailFor(propertyName)).build();
    }

    /**
     * Answers 400 when the database refused a value the caller supplied, and declines every other integrity failure.
     *
     * @throws DataIntegrityViolationException rethrown unchanged when no change to the payload could fix it
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnstorableValue(DataIntegrityViolationException ex) {
        String sqlState = sqlStateOf(ex);
        String detail = sqlState == null ? null : DETAIL_BY_SQL_STATE.get(sqlState);

        if (detail == null) {
            throw ex;
        }

        log.warn("rejected_unstorable_value sqlState={}", sqlState);

        return ErrorResponse.builder(ex, HttpStatus.BAD_REQUEST, detail).build();
    }

    private static String detailFor(String propertyName) {
        if (!SAFE_TO_ECHO.matcher(propertyName).matches()) {
            return UNNAMEABLE_PROPERTY_DETAIL;
        }

        return "Unknown sort property '" + propertyName + "'.";
    }

    private static @Nullable String sqlStateOf(Throwable ex) {
        Throwable cause = ex;

        while (cause != null) {
            if (cause instanceof SQLException driverFailure) {
                return driverFailure.getSQLState();
            }

            Throwable next = cause.getCause();
            cause = next == cause ? null : next;
        }

        return null;
    }
}
