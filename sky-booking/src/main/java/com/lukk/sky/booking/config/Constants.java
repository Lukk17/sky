package com.lukk.sky.booking.config;

import java.time.format.DateTimeFormatter;
import java.util.Set;

public class Constants {
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy");
    // keep with lower case
    public static final Set<String> USER_INFO_HEADERS = Set.of("x-auth-request-email", "x-forwarded-user");

    public static final String KAFKA_TOPIC = "bookingTopic-1";
}
