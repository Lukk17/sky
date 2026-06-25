package com.lukk.sky.booking.adapters.api;

import com.lukk.sky.booking.domain.exception.BookingException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that Resilience4j AOP annotations on {@link OfferServiceCaller} actually apply:
 * <ul>
 *   <li>A 5xx response is retried up to {@code max-attempts=3} times — confirmed by HTTP
 *       request count. After exhausting retries, {@link ResourceAccessException} propagates.</li>
 *   <li>A 404 surfaces as {@link BookingException} immediately without retry, because
 *       {@code BookingException} is in {@code ignore-exceptions}.</li>
 *   <li>A 200 returns the body on the happy path with exactly one request.</li>
 * </ul>
 *
 * <p>Uses a self-contained Spring context (no database or Kafka containers) so these tests
 * do not share circuit-breaker or retry state with the Testcontainers-based
 * {@code BookingIntegrationTest}. The {@link CircuitBreakerRegistry} is reset in
 * {@link BeforeEach} so each test starts from a clean circuit-breaker state.
 */
@SpringBootTest(
        classes = OfferServiceCallerResilienceTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@DisplayName("OfferServiceCaller Resilience4j integration tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OfferServiceCallerResilienceTest {

    /**
     * Minimal Spring configuration: enables Resilience4j autoconfiguration
     * and wires {@link OfferServiceCaller} with a MockWebServer-backed RestClient.
     * DataSource, JPA, and Kafka autoconfiguration are excluded so no external
     * infrastructure is required.
     */
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
        // Reset the circuit-breaker state between tests so failures from the 5xx test
        // do not carry over and open the circuit for subsequent tests.
        circuitBreakerRegistry.circuitBreaker("offerService").reset();
        requestCountBaseline = mockWebServer.getRequestCount();
    }

    @Test
    @Order(1)
    @DisplayName("5xx response is retried max-attempts times; ResourceAccessException surfaces after exhaustion")
    void callOfferService_whenServerReturns5xx_thenRetriesMaxAttemptsTimes() {
        // Enqueue three 5xx responses — one per attempt (max-attempts = 3).
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));

        String url = "http://localhost:" + mockWebServer.getPort() + "/api/internal/owner/offer/42";

        // ResourceAccessException propagates after retry exhaustion.
        assertThrows(
                ResourceAccessException.class,
                () -> offerServiceCaller.callOfferService(url, "42")
        );

        // Three requests must have been dispatched (one per retry attempt).
        assertEquals(3, mockWebServer.getRequestCount() - requestCountBaseline);
    }

    @Test
    @Order(2)
    @DisplayName("404 is not retried — BookingException surfaces immediately on first attempt")
    void callOfferService_whenServerReturns404_thenDoesNotRetryAndThrowsBookingException() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        String url = "http://localhost:" + mockWebServer.getPort() + "/api/internal/owner/offer/99";

        BookingException ex = assertThrows(
                BookingException.class,
                () -> offerServiceCaller.callOfferService(url, "99")
        );

        assertEquals("Offer not found for offerId=99", ex.getMessage());
        // Only one request — no retry on BookingException.
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

        String url = "http://localhost:" + mockWebServer.getPort() + "/api/internal/owner/offer/1";
        String owner = offerServiceCaller.callOfferService(url, "1");

        assertEquals("owner@example.com", owner);
        assertEquals(1, mockWebServer.getRequestCount() - requestCountBaseline);
    }
}
