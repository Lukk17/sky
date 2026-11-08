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
        // when / then
        assertThat(SecurityPaths.probes())
                .containsExactly("/actuator/health/**", "/actuator/info");
    }

    @Test
    @DisplayName("probes_doesNotExposePrometheus")
    void probes_doesNotExposePrometheus() {
        // when / then
        assertThat(SecurityPaths.probes())
                .as("the metrics endpoint must stay behind authentication")
                .noneMatch(path -> path.contains("prometheus"))
                .noneMatch("/actuator/**"::equals);
    }

    @Test
    @DisplayName("probesAndApiDocs_addsTheSpringdocPathsAfterTheProbes")
    void probesAndApiDocs_addsTheSpringdocPathsAfterTheProbes() {
        // when / then
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
        // when / then
        assertThat(SecurityPaths.apiDocs())
                .containsExactly("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html");
    }

    @Test
    @DisplayName("apiDocs_cannotBeMutatedByACaller")
    void apiDocs_cannotBeMutatedByACaller() {
        // when / then
        assertThatThrownBy(() -> SecurityPaths.apiDocs().add("/actuator/**"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("probesPlus_appendsServiceSpecificPaths")
    void probesPlus_appendsServiceSpecificPaths() {
        // when / then
        assertThat(SecurityPaths.probesPlus("/notifyWebsocket/**"))
                .containsExactly("/actuator/health/**", "/actuator/info", "/notifyWebsocket/**");
    }

    @Test
    @DisplayName("probes_cannotBeMutatedByACaller")
    void probes_cannotBeMutatedByACaller() {
        // when / then
        assertThatThrownBy(() -> SecurityPaths.probes().add("/actuator/**"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
