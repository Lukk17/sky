package com.lukk.sky.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SecurityPaths")
class SecurityPathsTest {

    @Test
    @DisplayName("probes_holdsOnlyTheHealthAndInfoEndpoints")
    void probes_holdsOnlyTheHealthAndInfoEndpoints() {
        assertThat(SecurityPaths.probes())
                .containsExactly("/actuator/health/**", "/actuator/info");
    }

    @Test
    @DisplayName("probes_doesNotExposePrometheus")
    void probes_doesNotExposePrometheus() {
        assertThat(SecurityPaths.probes())
                .as("the metrics endpoint must stay behind authentication")
                .noneMatch(path -> path.contains("prometheus"))
                .noneMatch("/actuator/**"::equals);
    }

    @Test
    @DisplayName("probesAndApiDocs_addsTheSpringdocPathsAfterTheProbes")
    void probesAndApiDocs_addsTheSpringdocPathsAfterTheProbes() {
        assertThat(SecurityPaths.probesAndApiDocs())
                .containsExactly(
                        "/actuator/health/**",
                        "/actuator/info",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html");
    }

    @Test
    @DisplayName("apiDocs_holdsOnlyTheSpringdocPaths")
    void apiDocs_holdsOnlyTheSpringdocPaths() {
        assertThat(SecurityPaths.apiDocs())
                .containsExactly("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html");
    }

    @Test
    @DisplayName("apiDocs_cannotBeMutatedByACaller")
    void apiDocs_cannotBeMutatedByACaller() {
        assertThatThrownBy(() -> SecurityPaths.apiDocs().add("/actuator/**"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("probesPlus_appendsServiceSpecificPaths")
    void probesPlus_appendsServiceSpecificPaths() {
        assertThat(SecurityPaths.probesPlus("/notifyWebsocket/**"))
                .containsExactly("/actuator/health/**", "/actuator/info", "/notifyWebsocket/**");
    }

    @Test
    @DisplayName("probes_cannotBeMutatedByACaller")
    void probes_cannotBeMutatedByACaller() {
        assertThatThrownBy(() -> SecurityPaths.probes().add("/actuator/**"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
