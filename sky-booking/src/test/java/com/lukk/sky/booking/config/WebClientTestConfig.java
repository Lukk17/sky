package com.lukk.sky.booking.config;

import okhttp3.mockwebserver.MockWebServer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;

/**
 * Starts an OkHttp {@link MockWebServer} on a free port and points the offer-service
 * configuration at it, so the URL {@code RequestUriStrategyPrimary} builds resolves to the
 * mock. The primary qualifier makes the {@link RestClient} override {@link RestClientConfig}.
 */
@TestConfiguration
public class WebClientTestConfig {

    @Bean(destroyMethod = "shutdown")
    public MockWebServer mockWebServer() {
        MockWebServer server = new MockWebServer();
        try {
            server.start(0);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to start MockWebServer", ex);
        }

        return server;
    }

    @Bean
    public DynamicPropertyRegistrar offerServicePortRegistrar(MockWebServer mockWebServer) {
        return registry -> registry.add("sky.offerServiceHostPort", mockWebServer::getPort);
    }

    @Bean
    @Primary
    public RestClient testRestClient(MockWebServer mockWebServer) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));

        return RestClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .requestFactory(factory)
                .build();
    }
}
