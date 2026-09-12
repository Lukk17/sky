package com.lukk.sky.common.startup;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StartupLogConfigTest {

    private ListAppender<ILoggingEvent> logAppender;
    private Logger startupLogger;

    @BeforeEach
    void attachAppender() {
        startupLogger = (Logger) LoggerFactory.getLogger(StartupLogConfig.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        startupLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachAppender() {
        startupLogger.detachAppender(logAppender);
    }

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

        // then: Eureka, service-discovery, and XSD sections must not appear
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

    @Test
    void onAcceptingTraffic_logsTheStartupBlock_whenTheApplicationStartsAcceptingTraffic() {
        // given
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.application.name", "ready-app")
                .withProperty("server.port", "5552");
        StartupLogConfig config = new StartupLogConfig(env);

        // when
        config.onAcceptingTraffic(new AvailabilityChangeEvent<>(this, ReadinessState.ACCEPTING_TRAFFIC));

        // then
        List<ILoggingEvent> infoEvents = logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.INFO)
                .toList();

        assertThat(infoEvents).hasSize(1);
        assertThat(infoEvents.get(0).getFormattedMessage())
                .contains("Application 'ready-app' is running!")
                .contains("http://localhost:5552");
    }

    @Test
    void onAcceptingTraffic_logsNothing_whenReadinessIsRefusingTraffic() {
        // given
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.application.name", "draining-app")
                .withProperty("server.port", "5552");
        StartupLogConfig config = new StartupLogConfig(env);

        // when
        config.onAcceptingTraffic(new AvailabilityChangeEvent<>(this, ReadinessState.REFUSING_TRAFFIC));

        // then
        assertThat(logAppender.list)
                .as("a service refusing traffic must not republish the startup block")
                .isEmpty();
    }

    @Test
    void buildStartupLog_prependsTheBanner_whenTheClasspathCarriesOne(@TempDir Path classpathRoot) throws IOException {
        // given
        Files.writeString(classpathRoot.resolve("banner.txt"), "SKY-BANNER-FIRST\nSKY-BANNER-SECOND\n");
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.application.name", "test-app")
                .withProperty("server.port", "8080");

        // when
        String result = buildWithContextClassLoader(directoryClassLoader(classpathRoot), env);

        // then
        assertThat(result).startsWith("SKY-BANNER-FIRST\nSKY-BANNER-SECOND\n");
        assertThat(result).contains("Application 'test-app' is running!");
    }

    @Test
    void buildStartupLog_dropsPlaceholderAndBlankBannerLines(@TempDir Path classpathRoot) throws IOException {
        // given
        Files.writeString(
                classpathRoot.resolve("banner.txt"),
                "SKY-BANNER-FIRST\n${application.version}\n\nSKY-BANNER-SECOND\n");
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.application.name", "test-app")
                .withProperty("server.port", "8080");

        // when
        String result = buildWithContextClassLoader(directoryClassLoader(classpathRoot), env);

        // then
        assertThat(result).startsWith("SKY-BANNER-FIRST\nSKY-BANNER-SECOND\n");
        assertThat(result)
                .as("an unresolved placeholder must never reach the log")
                .doesNotContain("${application.version}");
    }

    @Test
    void buildStartupLog_rendersWithoutABanner_whenTheBannerResourceCannotBeRead() {
        // given
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.application.name", "test-app")
                .withProperty("server.port", "8080");

        // when
        String result = buildWithContextClassLoader(new UnreadableResourceClassLoader(), env);

        // then
        assertThat(result).startsWith("------");
        assertThat(result).contains("Application 'test-app' is running!");
    }

    @Test
    void buildStartupLog_reportsTheJwkSetAsConnected_whenTheEndpointAnswers() throws IOException {
        // given
        HttpServer jwkEndpoint = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        jwkEndpoint.createContext("/certs", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        jwkEndpoint.start();
        String jwkSetUri = "http://localhost:" + jwkEndpoint.getAddress().getPort() + "/certs";

        try {
            MockEnvironment env = new MockEnvironment()
                    .withProperty("spring.application.name", "test-app")
                    .withProperty("server.port", "8080")
                    .withProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", jwkSetUri);

            // when
            String result = new StartupLogConfig(env).buildStartupLog();

            // then
            assertThat(result).contains("JWK Set:  " + jwkSetUri + " [Connected]");
            assertThat(result)
                    .as("a service configured with only a JWK set URI has no issuer to show")
                    .contains("Issuer:   (not configured)");

        } finally {
            jwkEndpoint.stop(0);
        }
    }

    @Test
    void buildStartupLog_reportsTheJwkSetAsFailed_whenTheEndpointIsUnreachable() throws IOException {
        // given
        int closedPort;
        try (ServerSocket reservation = new ServerSocket(0)) {
            closedPort = reservation.getLocalPort();
        }
        String jwkSetUri = "http://localhost:" + closedPort + "/certs";
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.application.name", "test-app")
                .withProperty("server.port", "8080")
                .withProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", jwkSetUri);

        // when
        String result = new StartupLogConfig(env).buildStartupLog();

        // then
        assertThat(result).contains("JWK Set:  " + jwkSetUri + " [FAILED]");
    }

    @Test
    void buildStartupLog_rendersWithoutABanner_whenTheBannerStreamFailsWhileBeingRead() {
        // given
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.application.name", "test-app")
                .withProperty("server.port", "8080");

        // when
        String result = buildWithContextClassLoader(new FailingStreamClassLoader(), env);

        // then
        assertThat(result).startsWith("------");
        assertThat(result).contains("Application 'test-app' is running!");
    }

    @Test
    void buildStartupLog_fallsBackToLocalhost_whenTheHostNameCannotBeResolved() {
        // given
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.application.name", "test-app")
                .withProperty("server.port", "8080");
        StartupLogConfig config = new UnresolvableHostNameStartupLogConfig(env);

        // when
        String result = config.buildStartupLog();

        // then
        assertThat(result)
                .as("a host with no resolvable name still gets a usable hostname URL")
                .contains("Hostname:  http://localhost:8080");
        assertThat(result)
                .as("the runtime block names the fallback rather than an empty container line")
                .contains("Container: localhost");
        assertThat(result)
                .as("the banner still renders in full")
                .contains("Application 'test-app' is running!")
                .endsWith("----------------------------------------------------------");
    }

    private static String buildWithContextClassLoader(ClassLoader loader, MockEnvironment env) {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);

        try {
            return new StartupLogConfig(env).buildStartupLog();

        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    private static ClassLoader directoryClassLoader(Path directory) throws IOException {
        URL directoryUrl = directory.toUri().toURL();

        return new URLClassLoader(new URL[]{directoryUrl}, null);
    }

    private static final class UnresolvableHostNameStartupLogConfig extends StartupLogConfig {

        private UnresolvableHostNameStartupLogConfig(MockEnvironment env) {
            super(env);
        }

        @Override
        String resolveHostName() throws UnknownHostException {
            throw new UnknownHostException("no reverse mapping for this host");
        }
    }

    private static final class FailingStreamClassLoader extends ClassLoader {

        private FailingStreamClassLoader() {
            super(null);
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            return new InputStream() {

                @Override
                public int read() throws IOException {
                    throw new IOException("banner stream truncated: " + name);
                }
            };
        }
    }

    private static final class UnreadableResourceClassLoader extends ClassLoader {

        private UnreadableResourceClassLoader() {
            super(null);
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            throw new IllegalStateException("classpath resource unavailable: " + name);
        }
    }
}
