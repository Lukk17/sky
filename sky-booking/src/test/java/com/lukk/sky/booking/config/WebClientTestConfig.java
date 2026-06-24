package com.lukk.sky.booking.config;

import okhttp3.mockwebserver.MockWebServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;

/**
 * Test configuration that starts an OkHttp {@link MockWebServer} on the configured
 * offer-service port and wires a synchronous {@link RestClient} pointing at it.
 * The primary qualifier ensures it overrides the production {@link RestClientConfig} bean.
 * MockWebServer shutdown is handled via its own destroy method registered on the bean.
 */
@TestConfiguration
public class WebClientTestConfig {

    @Value("${sky.offerServiceHostPort}")
    int hostPort;

    @Bean(destroyMethod = "shutdown")
    public MockWebServer mockWebServer() {
        MockWebServer server = new MockWebServer();
        try {
            server.start(hostPort);
        } catch (IOException e) {
            throw new RuntimeException("Failed to start MockWebServer", e);
        }

        return server;
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
