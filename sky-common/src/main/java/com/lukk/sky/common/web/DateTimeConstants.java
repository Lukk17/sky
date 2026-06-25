package com.lukk.sky.common.web;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateTimeConstants {

    public static final ZoneId DISPLAY_ZONE = ZoneId.of("Europe/Warsaw");

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy").withZone(DISPLAY_ZONE);

    private DateTimeConstants() {
    }
}
