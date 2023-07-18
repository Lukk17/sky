package com.lukk.sky.message.config;

import java.time.format.DateTimeFormatter;
import java.util.Set;

public class Constants {
    public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy");

    public static final Set<String> USER_INFO_HEADERS = Set.of("x-auth-request-email", "x-forwarded-user");

}
