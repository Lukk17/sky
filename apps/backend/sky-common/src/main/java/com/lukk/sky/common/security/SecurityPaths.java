package com.lukk.sky.common.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityPaths {

    private static final List<String> PROBES = List.of(
            "/actuator/health/**",
            "/actuator/info");

    private static final List<String> API_DOCS = List.of(
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html");

    private static final List<String> PROBES_AND_API_DOCS =
            Stream.concat(PROBES.stream(), API_DOCS.stream()).toList();

    public static List<String> probes() {
        return PROBES;
    }

    public static List<String> apiDocs() {
        return API_DOCS;
    }

    public static List<String> probesAndApiDocs() {
        return PROBES_AND_API_DOCS;
    }

    public static List<String> probesPlus(String... additionalPatterns) {
        return Stream.concat(PROBES.stream(), Stream.of(additionalPatterns)).toList();
    }
}
