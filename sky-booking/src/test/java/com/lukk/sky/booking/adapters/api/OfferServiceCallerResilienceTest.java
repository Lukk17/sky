package com.lukk.sky.booking.adapters.api;

import com.lukk.sky.booking.domain.exception.BookingException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(
        classes = OfferServiceCallerResilienceTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@DisplayName("OfferServiceCaller Resilience4j integration tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OfferServiceCallerResilienceTest {

    @Configuration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            KafkaAutoConfiguration.class
    })
    @Import(OfferServiceCaller.class)
    static class TestConfig {

        @Bean(destroyMethod = "shutdown")
        MockWebServer mockWebServer() throws IOException {
            MockWebServer server = new MockWebServer();
            server.start(0);
            return server;
        }

        @Bean
        RestClient restClient(MockWebServer mockWebServer) {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Duration.ofSeconds(2));
            factory.setReadTimeout(Duration.ofSeconds(3));
            return RestClient.builder()
                    .requestFactory(factory)
                    .build();
        }
    }

    @Autowired
    private OfferServiceCaller offerServiceCaller;

    @Autowired
    private MockWebServer mockWebServer;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private int requestCountBaseline;

    @BeforeEach
    void resetState() {
        circuitBreakerRegistry.circuitBreaker("offerService").reset();
        requestCountBaseline = mockWebServer.getRequestCount();
    }

    @Test
    @Order(1)
    @DisplayName("5xx response is retried max-attempts times; ResourceAccessException surfaces after exhaustion")
    void callOfferService_whenServerReturns5xx_thenRetriesMaxAttemptsTimes() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));

        String url = "http://localhost:" + mockWebServer.getPort() + "/api/v1/offers/42/owner";

        assertThrows(
                ResourceAccessException.class,
                () -> offerServiceCaller.callOfferService(url, UUID.randomUUID())
        );

        assertEquals(3, mockWebServer.getRequestCount() - requestCountBaseline);
    }

    @Test
    @Order(2)
    @DisplayName("404 is not retried — BookingException surfaces immediately on first attempt")
    void callOfferService_whenServerReturns404_thenDoesNotRetryAndThrowsBookingException() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        UUID testOfferId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        String url = "http://localhost:" + mockWebServer.getPort() + "/api/v1/offers/99/owner";

        BookingException ex = assertThrows(
                BookingException.class,
                () -> offerServiceCaller.callOfferService(url, testOfferId)
        );

        assertEquals("Offer not found for offerId=" + testOfferId, ex.getMessage());
        assertEquals(1, mockWebServer.getRequestCount() - requestCountBaseline);
    }

    @Test
    @Order(3)
    @DisplayName("200 response returns body directly on happy path")
    void callOfferService_whenServerReturns200_thenReturnsBody() {
        mockWebServer.enqueue(
                new MockResponse()
                        .setBody("owner@example.com")
                        .setResponseCode(200)
        );

        String url = "http://localhost:" + mockWebServer.getPort() + "/api/v1/offers/1/owner";
        String owner = offerServiceCaller.callOfferService(url, UUID.randomUUID());

        assertEquals("owner@example.com", owner);
        assertEquals(1, mockWebServer.getRequestCount() - requestCountBaseline);
    }
}
