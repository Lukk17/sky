package com.lukk.sky.common.startup;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class StartupLogConfigTest {

    @Test
    void buildStartupLog_containsAccessUrlsAndRequiredSections() {
        // given
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.application.name", "test-app")
                .withProperty("server.port", "8080")
                .withProperty("springdoc.api-docs.path", "/v3/api-docs");
        StartupLogConfig config = new StartupLogConfig(env);

        // when
        String result = config.buildStartupLog();

        // then
        assertThat(result).contains("Application 'test-app' is running!");
        assertThat(result).contains("Access URLs:");
        assertThat(result).contains("Local:     http://localhost:8080");
        assertThat(result).contains("Profile(s):");
        assertThat(result).contains("Actuator:");
        assertThat(result).contains("Health:");
        assertThat(result).contains("Readiness:");
        assertThat(result).contains("Prometheus:");
        assertThat(result).contains("API documentation:");
        assertThat(result).contains("/v3/api-docs");
        assertThat(result).contains("swagger-ui/index.html");
        assertThat(result).contains("Observability:");
        assertThat(result).contains("Tracing:");
        assertThat(result).contains("Logging:");
    }

    @Test
    void buildStartupLog_doesNotContainDroppedSections() {
        // given
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.application.name", "test-app")
                .withProperty("server.port", "8080");
        StartupLogConfig config = new StartupLogConfig(env);

        // when
        String result = config.buildStartupLog();

        // then — Eureka, service-discovery, and XSD sections must not appear
        assertThat(result).doesNotContainIgnoringCase("eureka");
        assertThat(result).doesNotContainIgnoringCase("service discovery");
        assertThat(result).doesNotContainIgnoringCase("xsd");
        assertThat(result).doesNotContainIgnoringCase("xml schema");
    }

    @Test
    void buildStartupLog_withOAuth2IssuerUri_includesAuthSection() {
        // given
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.application.name", "test-app")
                .withProperty("server.port", "8080")
                .withProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                        "https://keycloak.test:9443/realms/sky");
        StartupLogConfig config = new StartupLogConfig(env);

        // when
        String result = config.buildStartupLog();

        // then
        assertThat(result).contains("Auth (OAuth2 Resource Server):");
        assertThat(result).contains("Issuer:   https://keycloak.test:9443/realms/sky");
        assertThat(result).contains("JWK Set:  (not configured)");
    }

    @Test
    void buildStartupLog_withoutOAuth2Properties_omitsAuthSection() {
        // given
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.application.name", "test-app")
                .withProperty("server.port", "8080");
        StartupLogConfig config = new StartupLogConfig(env);

        // when
        String result = config.buildStartupLog();

        // then
        assertThat(result).doesNotContain("Auth (OAuth2 Resource Server):");
    }

    @Test
    void buildStartupLog_withSpringdocApiDocsPath_showsRealUrls() {
        // given
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.application.name", "test-app")
                .withProperty("server.port", "8080")
                .withProperty("springdoc.api-docs.path", "/v3/api-docs");
        StartupLogConfig config = new StartupLogConfig(env);

        // when
        String result = config.buildStartupLog();

        // then
        assertThat(result).contains("OpenAPI:    http://localhost:8080/v3/api-docs");
        assertThat(result).contains("Swagger UI: http://localhost:8080/swagger-ui/index.html");
    }

    @Test
    void buildStartupLog_withoutSpringdocApiDocsPath_showsNoSpringdocUi() {
        // given
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.application.name", "test-app")
                .withProperty("server.port", "8080");
        StartupLogConfig config = new StartupLogConfig(env);

        // when
        String result = config.buildStartupLog();

        // then
        assertThat(result).contains("API documentation:");
        assertThat(result).contains("(no springdoc UI)");
    }

    @Test
    void buildStartupLog_withSslKeyStore_usesHttps() {
        // given
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.application.name", "test-app")
                .withProperty("server.port", "8443")
                .withProperty("server.ssl.key-store", "classpath:keystore.p12");
        StartupLogConfig config = new StartupLogConfig(env);

        // when
        String result = config.buildStartupLog();

        // then
        assertThat(result).contains("https://localhost:8443");
    }

    @Test
    void buildStartupLog_withContextPath_includesItInUrls() {
        // given
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.application.name", "test-app")
                .withProperty("server.port", "8080")
                .withProperty("server.servlet.context-path", "/sky");
        StartupLogConfig config = new StartupLogConfig(env);

        // when
        String result = config.buildStartupLog();

        // then
        assertThat(result).contains("http://localhost:8080/sky");
    }

    @Test
    void formatProfiles_withActiveProfiles_returnsCommaJoined() {
        // given
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("local", "debug");
        StartupLogConfig config = new StartupLogConfig(env);

        // when
        String result = config.formatProfiles();

        // then
        assertThat(result).isEqualTo("local, debug");
    }

    @Test
    void formatProfiles_withNoActiveProfiles_returnsDefault() {
        // given
        MockEnvironment env = new MockEnvironment();
        StartupLogConfig config = new StartupLogConfig(env);

        // when
        String result = config.formatProfiles();

        // then
        assertThat(result).isEqualTo("default");
    }

    @Test
    void describeTracing_withOtlpEndpoint_includesEndpointAndSampling() {
        // given
        MockEnvironment env = new MockEnvironment()
                .withProperty("management.otlp.tracing.endpoint", "http://otel:4318/v1/traces")
                .withProperty("management.tracing.sampling.probability", "0.5");
        StartupLogConfig config = new StartupLogConfig(env);

        // when
        String result = config.describeTracing();

        // then
        assertThat(result).contains("http://otel:4318/v1/traces");
        assertThat(result).contains("sampling=0.5");
    }

    @Test
    void describeTracing_withoutOtlpEndpoint_reportsNoEndpoint() {
        // given
        MockEnvironment env = new MockEnvironment();
        StartupLogConfig config = new StartupLogConfig(env);

        // when
        String result = config.describeTracing();

        // then
        assertThat(result).contains("no OTLP endpoint");
        assertThat(result).contains("sampling=1.0");
    }

    @Test
    void describeLogging_withLocalProfile_returnsTextPattern() {
        // given
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("local");
        StartupLogConfig config = new StartupLogConfig(env);

        // when
        String result = config.describeLogging();

        // then
        assertThat(result).contains("text pattern");
    }

    @Test
    void describeLogging_withTestProfile_returnsTextPattern() {
        // given
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        StartupLogConfig config = new StartupLogConfig(env);

        // when
        String result = config.describeLogging();

        // then
        assertThat(result).contains("text pattern");
    }

    @Test
    void describeLogging_withProductionProfile_returnsJson() {
        // given
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        StartupLogConfig config = new StartupLogConfig(env);

        // when
        String result = config.describeLogging();

        // then
        assertThat(result).contains("JSON");
    }

    @Test
    void buildStartupLog_inKubernetes_showsPodAndNamespace() {
        // given
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.application.name", "test-app")
                .withProperty("server.port", "8080")
                .withProperty("KUBERNETES_SERVICE_HOST", "10.43.0.1")
                .withProperty("POD_NAME", "sky-offer-deployment-abc123")
                .withProperty("POD_NAMESPACE", "default");
        StartupLogConfig config = new StartupLogConfig(env);

        // when
        String result = config.buildStartupLog();

        // then
        assertThat(result).contains("Runtime:");
        assertThat(result).contains("Mode:      Kubernetes");
        assertThat(result).contains("Pod:       sky-offer-deployment-abc123");
        assertThat(result).contains("Namespace: default");
    }

    @Test
    void buildStartupLog_outsideKubernetes_showsStandaloneMode() {
        // given
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.application.name", "test-app")
                .withProperty("server.port", "8080");
        StartupLogConfig config = new StartupLogConfig(env);

        // when
        String result = config.buildStartupLog();

        // then
        assertThat(result).contains("Runtime:");
        assertThat(result).contains("standalone / Docker Compose");
        assertThat(result).doesNotContain("Mode:      Kubernetes");
    }
}
