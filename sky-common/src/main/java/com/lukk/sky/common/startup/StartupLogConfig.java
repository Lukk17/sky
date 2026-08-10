package com.lukk.sky.common.startup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@AutoConfiguration
@RequiredArgsConstructor
@Slf4j
public class StartupLogConfig {

    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(2);
    private static final String BANNER_RESOURCE = "banner.txt";
    private static final String SEP = "----------------------------------------------------------";

    private final Environment env;

    @EventListener
    public void onAcceptingTraffic(AvailabilityChangeEvent<ReadinessState> event) {
        if (event.getState() != ReadinessState.ACCEPTING_TRAFFIC) {
            return;
        }

        log.info("\n{}", buildStartupLog());
    }

    String buildStartupLog() {
        String appName = env.getProperty("spring.application.name", "app");
        String port = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        boolean ssl = env.containsProperty("server.ssl.key-store");
        String protocol = ssl ? "https" : "http";
        String hostname = localHostName();

        String localUrl = protocol + "://localhost:" + port + contextPath;
        String hostnameUrl = protocol + "://" + hostname + ":" + port + contextPath;

        StringBuilder sb = new StringBuilder();

        String banner = loadBanner();
        if (!banner.isEmpty()) {
            sb.append(banner).append('\n');
        }

        sb.append(SEP).append('\n');
        sb.append("    Application '").append(appName).append("' is running!\n");
        sb.append('\n');
        sb.append("    Access URLs:\n");
        sb.append("      Local:     ").append(localUrl).append('\n');
        sb.append("      Hostname:  ").append(hostnameUrl).append('\n');
        sb.append('\n');
        sb.append("    Profile(s): ").append(formatProfiles()).append('\n');
        sb.append('\n');
        sb.append(buildRuntimeBlock());

        String oauth2Block = buildOAuth2Block();
        if (!oauth2Block.isEmpty()) {
            sb.append('\n');
            sb.append(oauth2Block);
        }

        sb.append('\n');
        sb.append(buildActuatorBlock(localUrl));
        sb.append('\n');
        sb.append(buildApiDocsBlock(localUrl));
        sb.append('\n');
        sb.append(buildObservabilityBlock());
        sb.append(SEP);

        return sb.toString();
    }

    String formatProfiles() {
        String[] profiles = env.getActiveProfiles();
        if (profiles.length == 0) {
            return "default";
        }

        return String.join(", ", profiles);
    }

    String describeTracing() {
        String otlpEndpoint = env.getProperty("management.otlp.tracing.endpoint");
        String sampling = env.getProperty("management.tracing.sampling.probability", "1.0");

        if (otlpEndpoint != null) {
            return "OTel OTLP → " + otlpEndpoint + " (sampling=" + sampling + ")";
        }

        return "OTel bridge enabled, no OTLP endpoint set (sampling=" + sampling + ")";
    }

    String describeLogging() {
        List<String> profiles = Arrays.asList(env.getActiveProfiles());
        if (profiles.contains("local") || profiles.contains("test")) {
            return "text pattern [local/test profile]";
        }

        return "JSON (logstash-logback-encoder)";
    }

    private String loadBanner() {
        try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(BANNER_RESOURCE)) {
            if (is == null) {
                return "";
            }

            return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .filter(line -> !line.contains("${") && !line.isBlank())
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.debug("Could not load {}: {}", BANNER_RESOURCE, e.getMessage());
            return "";
        }
    }

    private String buildRuntimeBlock() {
        String host = localHostName();
        boolean inKubernetes = env.containsProperty("KUBERNETES_SERVICE_HOST");

        if (!inKubernetes) {
            return "    Runtime:\n"
                    + "      Mode:      standalone / Docker Compose (Spring Cloud Gateway front, no service registry)\n"
                    + "      Container: " + host + "\n";
        }

        String pod = env.getProperty("POD_NAME", env.getProperty("HOSTNAME", host));
        String namespace = env.getProperty("POD_NAMESPACE", "(expose POD_NAMESPACE via the downward API to show)");

        return "    Runtime:\n"
                + "      Mode:      Kubernetes (nginx ingress front, no service registry)\n"
                + "      Pod:       " + pod + "\n"
                + "      Namespace: " + namespace + "\n";
    }

    private String buildOAuth2Block() {
        String issuerUri = env.getProperty(
                "spring.security.oauth2.resourceserver.jwt.issuer-uri");
        String jwkSetUri = env.getProperty(
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri");

        if (issuerUri == null && jwkSetUri == null) {
            return "";
        }

        String issuerDisplay = issuerUri != null ? issuerUri : "(not configured)";
        String jwkDisplay = jwkSetUri != null
                ? jwkSetUri + " " + probeHttpEndpoint(jwkSetUri)
                : "(not configured)";

        return "    Auth (OAuth2 Resource Server):\n"
                + "      Issuer:   " + issuerDisplay + "\n"
                + "      JWK Set:  " + jwkDisplay + "\n";
    }

    private String buildActuatorBlock(String baseUrl) {
        return "    Actuator:\n"
                + "      Health:     " + baseUrl + "/actuator/health\n"
                + "      Readiness:  " + baseUrl + "/actuator/health/readiness\n"
                + "      Prometheus: " + baseUrl + "/actuator/prometheus\n"
                + "      Metrics:    " + baseUrl + "/actuator/metrics\n";
    }

    private String buildApiDocsBlock(String baseUrl) {
        String apiDocsPath = env.getProperty("springdoc.api-docs.path");
        if (apiDocsPath == null) {
            return "    API documentation:\n"
                    + "      (no springdoc UI)\n";
        }

        String swaggerUiPath = env.getProperty("springdoc.swagger-ui.path", "/swagger-ui/index.html");

        return "    API documentation:\n"
                + "      OpenAPI:    " + baseUrl + apiDocsPath + "\n"
                + "      Swagger UI: " + baseUrl + swaggerUiPath + "\n";
    }

    private String buildObservabilityBlock() {
        return "    Observability:\n"
                + "      Tracing:  " + describeTracing() + "\n"
                + "      Logging:  " + describeLogging() + "\n";
    }

    private String probeHttpEndpoint(String url) {
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(PROBE_TIMEOUT);
            factory.setReadTimeout(PROBE_TIMEOUT);
            RestClient client = RestClient.builder().requestFactory(factory).build();
            client.get().uri(url).retrieve().toBodilessEntity();
            return "[Connected]";
        } catch (Exception e) {
            log.debug("Startup probe failed for {}: {}", url, e.getMessage());
            return "[FAILED]";
        }
    }

    private String localHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.debug("Could not resolve local hostname: {}", e.getMessage());
            return "localhost";
        }
    }
}
