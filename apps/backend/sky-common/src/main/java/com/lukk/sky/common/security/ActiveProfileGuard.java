package com.lukk.sky.common.security;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
public class ActiveProfileGuard {

    static final Set<String> RECOGNISED_PROFILES = Set.of("default", "local", "prod", "test");

    private final Environment environment;

    @PostConstruct
    void rejectUnrecognisedProfiles() {
        TreeSet<String> unrecognised = new TreeSet<>(Arrays.asList(environment.getActiveProfiles()));
        unrecognised.removeAll(RECOGNISED_PROFILES);
        if (unrecognised.isEmpty()) {
            return;
        }

        String leftover = String.join(", ", unrecognised);
        String recognised = RECOGNISED_PROFILES.stream()
                .sorted()
                .collect(Collectors.joining(", "));
        String message = ("Unrecognised active Spring profile(s): %s. No security filter chain claims %s, so the "
                + "application would start behind Spring Boot's fallback chain. Recognised profiles: %s, or none "
                + "for the default chain. Add the new profile to the main security config's @Profile expression "
                + "and to ActiveProfileGuard.RECOGNISED_PROFILES, in that order.")
                .formatted(leftover, leftover, recognised);
        throw new IllegalStateException(message);
    }
}
