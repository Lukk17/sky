package com.lukk.sky.common.config;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class RequiredCredentials {

    private final List<MissingCredential> missing = new ArrayList<>();

    private RequiredCredentials() {
    }

    public static RequiredCredentials check() {
        return new RequiredCredentials();
    }

    public RequiredCredentials and(String property, @Nullable String value) {
        MissingCredential.from(property, value).ifPresent(missing::add);

        return this;
    }

    public void orFailStartup() {
        if (missing.isEmpty()) {
            return;
        }

        throw new MissingCredentialException(missing);
    }
}
