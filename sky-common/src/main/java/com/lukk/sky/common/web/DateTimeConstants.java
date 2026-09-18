package com.lukk.sky.common.web;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DateTimeConstants {

    public static final ZoneId DISPLAY_ZONE = ZoneId.of("Europe/Warsaw");

    /**
     * Formats and parses a calendar date, meaning a {@code LocalDate}, as {@code yyyy-MM-dd}.
     */
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Formats and parses a point in time, meaning an {@code Instant}, rendered in {@link #DISPLAY_ZONE}.
     */
    public static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy").withZone(DISPLAY_ZONE);
}
