package com.lukk.sky.common.config;

import org.jspecify.annotations.Nullable;

import java.util.Optional;

public record MissingCredential(String environmentVariable, String property) {

    private static final String PLACEHOLDER_START = "${";
    private static final String PLACEHOLDER_END = "}";

    static Optional<MissingCredential> from(String property, @Nullable String value) {
        return unresolvedVariableName(value)
                .map(variable -> new MissingCredential(variable, property));
    }

    public String unresolvedValue() {
        return PLACEHOLDER_START + environmentVariable + PLACEHOLDER_END;
    }

    public String describe() {
        return "%s is still the literal text %s, because environment variable %s is not set"
                .formatted(property, quoted(unresolvedValue()), environmentVariable);
    }

    private static Optional<String> unresolvedVariableName(@Nullable String value) {
        if (value == null) {
            return Optional.empty();
        }

        String trimmed = value.trim();
        if (!trimmed.startsWith(PLACEHOLDER_START) || !trimmed.endsWith(PLACEHOLDER_END)) {
            return Optional.empty();
        }

        String variable = trimmed.substring(PLACEHOLDER_START.length(), trimmed.length() - PLACEHOLDER_END.length());
        if (variable.isEmpty() || containsPlaceholderSyntax(variable)) {
            return Optional.empty();
        }

        return Optional.of(variable);
    }

    private static boolean containsPlaceholderSyntax(String variable) {
        return variable.indexOf('{') >= 0 || variable.indexOf('}') >= 0 || variable.indexOf(':') >= 0;
    }

    private static String quoted(String value) {
        return '"' + value + '"';
    }
}
