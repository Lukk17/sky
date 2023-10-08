package com.lukk.sky.booking.config;

import jakarta.annotation.PreDestroy;
import okhttp3.mockwebserver.MockWebServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

@TestConfiguration
public class WebClientTestConfig {

    @Value("${sky.offerServiceHostPort}")
    int hostPort;

    @Bean
    public MockWebServer mockWebServer() {
        MockWebServer server = new MockWebServer();
        try {
            server.start(hostPort);  // Starts MockWebServer on port 8080
        } catch (IOException e) {
            throw new RuntimeException("Failed to start MockWebServer", e);
        }
        return server;
    }

    @PreDestroy
    public void tearDown() throws IOException {
        mockWebServer().shutdown();
    }

    @Bean
    public WebClient testWebClient(MockWebServer mockWebServer) {
        return WebClient.builder().baseUrl(mockWebServer.url("/").toString()).build();
    }
}
