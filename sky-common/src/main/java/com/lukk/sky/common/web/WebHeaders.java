package com.lukk.sky.common.web;

import java.util.Set;

public final class WebHeaders {

    public static final Set<String> USER_INFO_HEADERS = Set.of("x-auth-request-email", "x-forwarded-user");

    private WebHeaders() {
    }
}
