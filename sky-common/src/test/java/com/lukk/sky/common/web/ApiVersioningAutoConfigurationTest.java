package com.lukk.sky.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.accept.ApiVersionStrategy;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.ServletRequestPathUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ApiVersioningAutoConfiguration")
class ApiVersioningAutoConfigurationTest {

    private final ApiVersionStrategy strategy = configuredStrategy();

    @Test
    @DisplayName("configureApiVersioning_registersAVersionStrategy")
    void configureApiVersioning_registersAVersionStrategy() {
        assertThat(strategy)
                .as("no strategy means no request is ever version-resolved")
                .isNotNull();
    }

    @Test
    @DisplayName("resolveVersion_returnsTheSecondSegment_whenItIsAVersion")
    void resolveVersion_returnsTheSecondSegment_whenItIsAVersion() {
        assertThat(strategy.resolveVersion(request("/api/v1/offers"))).isEqualTo("v1");
    }

    @Test
    @DisplayName("resolveVersion_returnsTheSecondSegment_whenTheVersionHasSeveralDigits")
    void resolveVersion_returnsTheSecondSegment_whenTheVersionHasSeveralDigits() {
        assertThat(strategy.resolveVersion(request("/api/v12/offers"))).isEqualTo("v12");
    }

    @Test
    @DisplayName("resolveVersion_returnsNoVersion_whenTheSecondSegmentIsAResourceName")
    void resolveVersion_returnsNoVersion_whenTheSecondSegmentIsAResourceName() {
        assertThat(strategy.resolveVersion(request("/api/offers/42")))
                .as("an unversioned path must fall through to the default version, not be read as version 'offers'")
                .isNull();
    }

    @Test
    @DisplayName("resolveVersion_returnsNoVersion_whenTheSecondSegmentStartsWithVButIsAWord")
    void resolveVersion_returnsNoVersion_whenTheSecondSegmentStartsWithVButIsAWord() {
        assertThat(strategy.resolveVersion(request("/api/version/offers"))).isNull();
    }

    @Test
    @DisplayName("resolveVersion_returnsNoVersion_whenTheSecondSegmentIsALoneV")
    void resolveVersion_returnsNoVersion_whenTheSecondSegmentIsALoneV() {
        assertThat(strategy.resolveVersion(request("/api/v/offers"))).isNull();
    }

    @Test
    @DisplayName("resolveVersion_returnsNoVersion_whenTheVersionSitsInTheFirstSegment")
    void resolveVersion_returnsNoVersion_whenTheVersionSitsInTheFirstSegment() {
        assertThat(strategy.resolveVersion(request("/v1/offers")))
                .as("the version is read from the second segment, so /v1/... carries none")
                .isNull();
    }

    @Test
    @DisplayName("resolveVersion_returnsNoVersion_whenThePathHasOnlyOneSegment")
    void resolveVersion_returnsNoVersion_whenThePathHasOnlyOneSegment() {
        assertThat(strategy.resolveVersion(request("/api"))).isNull();
    }

    @Test
    @DisplayName("resolveVersion_returnsNoVersion_whenThePathHasNoSegments")
    void resolveVersion_returnsNoVersion_whenThePathHasNoSegments() {
        assertThat(strategy.resolveVersion(request("/"))).isNull();
    }

    @Test
    @DisplayName("getDefaultVersion_isVersionOne")
    void getDefaultVersion_isVersionOne() {
        assertThat(strategy.getDefaultVersion()).isEqualTo(strategy.parseVersion("1"));
    }

    @Test
    @DisplayName("validateVersion_accepts_whenTheRequestAsksForVersionOne")
    void validateVersion_accepts_whenTheRequestAsksForVersionOne() {
        HttpServletRequest request = request("/api/v1/offers");

        assertThatCode(() -> strategy.validateVersion(strategy.parseVersion("1"), request))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateVersion_rejects_whenTheRequestAsksForAnUnsupportedVersion")
    void validateVersion_rejects_whenTheRequestAsksForAnUnsupportedVersion() {
        HttpServletRequest request = request("/api/v2/offers");

        assertThatThrownBy(() -> strategy.validateVersion(strategy.parseVersion("2"), request))
                .isInstanceOf(InvalidApiVersionException.class);
    }

    @Test
    @DisplayName("registersTheConfigurer_inAServletWebApplication")
    void registersTheConfigurer_inAServletWebApplication() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ApiVersioningAutoConfiguration.class))
                .run(context -> assertThat(context).hasSingleBean(WebMvcConfigurer.class));
    }

    @Test
    @DisplayName("registersNothing_outsideAServletWebApplication")
    void registersNothing_outsideAServletWebApplication() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ApiVersioningAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(WebMvcConfigurer.class));
    }

    private static ApiVersionStrategy configuredStrategy() {
        StrategyExposingConfigurer configurer = new StrategyExposingConfigurer();
        new ApiVersioningAutoConfiguration().configureApiVersioning(configurer);

        return configurer.strategy();
    }

    private static HttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        ServletRequestPathUtils.parseAndCache(request);

        return request;
    }

    private static final class StrategyExposingConfigurer extends ApiVersionConfigurer {

        private ApiVersionStrategy strategy() {
            return getApiVersionStrategy();
        }
    }
}
