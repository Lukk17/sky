package com.lukk.sky.notify.config;

import com.lukk.sky.common.web.UnhandledExceptionResolver;
import com.lukk.sky.notify.AbstractIntegrationTest;
import com.lukk.sky.notify.TestSecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This service has a servlet container only because it serves a WebSocket handshake, so the shared resolver is
 * registered here too. Inert means the context still starts, nothing routes through it, and the handshake answers
 * as it did.
 */
@DisplayName("sky-notify is unaffected by the shared last-resort resolver")
@Import(TestSecurityConfig.class)
class UnhandledExceptionResolverInertTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    @DisplayName("contextStarts_andTheResolverIsAmongTheResolversTheDispatcherServletDetected")
    void contextStarts_andTheResolverIsAmongTheResolversTheDispatcherServletDetected() {
        assertThat(context.getBeansOfType(HandlerExceptionResolver.class).values())
                .as("a servlet resolver in a servlet application is wiring, not a failure")
                .hasAtLeastOneElementOfType(UnhandledExceptionResolver.class);
    }

    @Test
    @DisplayName("noRequestMappingOfThisServiceExists_soNothingCanEverReachTheResolver")
    void noRequestMappingOfThisServiceExists_soNothingCanEverReachTheResolver() {
        RequestMappingHandlerMapping mappings =
                context.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);

        assertThat(mappings.getHandlerMethods().values().stream().map(HandlerMethod::getBeanType))
                .as("this service exposes no REST surface, so the resolver has nothing to be last behind")
                .noneMatch(beanType -> beanType.getName().startsWith("com.lukk.sky.notify"));
    }

    @Test
    @DisplayName("webSocketHandshakeEndpointStillAnswers_ratherThanAServerError")
    void webSocketHandshakeEndpointStillAnswers_ratherThanAServerError() throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + port + "/notifyWebsocket/info"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode())
                .as("the handshake path must not be touched by an exception resolver it never reaches")
                .isLessThan(500);
    }
}
