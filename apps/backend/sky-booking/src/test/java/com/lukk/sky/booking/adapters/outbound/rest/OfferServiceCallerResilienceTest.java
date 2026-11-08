package com.lukk.sky.booking.adapters.outbound.rest;

import com.lukk.sky.booking.domain.exception.OfferNotFoundException;
import com.lukk.sky.booking.domain.exception.OfferServiceBadResponseException;
import com.lukk.sky.booking.domain.exception.OfferServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
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
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
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

    private static final Duration TEST_CONNECT_TIMEOUT = Duration.ofMillis(500);
    private static final Duration TEST_READ_TIMEOUT = Duration.ofMillis(500);
    private static final int SLIDING_WINDOW_SIZE = 10;

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
        RestClient restClient() {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(TEST_CONNECT_TIMEOUT)
                    .build();

            JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
            factory.setReadTimeout(TEST_READ_TIMEOUT);

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
    @DisplayName("5xx response is retried max-attempts times. ResourceAccessException surfaces after exhaustion")
    void callOfferService_whenServerReturns5xx_thenRetriesMaxAttemptsTimes() {
        // given
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        String url = "http://localhost:" + mockWebServer.getPort() + "/api/v1/offers/42/owner";

        // when / then
        assertThrows(
                ResourceAccessException.class,
                () -> offerServiceCaller.callOfferService(url, UUID.randomUUID())
        );

        assertEquals(3, mockWebServer.getRequestCount() - requestCountBaseline);
    }

    @Test
    @Order(2)
    @DisplayName("404 is not retried: OfferNotFoundException surfaces immediately on first attempt")
    void callOfferService_whenServerReturns404_thenDoesNotRetryAndThrowsOfferNotFoundException() {
        // given
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));
        UUID testOfferId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        String url = "http://localhost:" + mockWebServer.getPort() + "/api/v1/offers/99/owner";

        // when / then
        OfferNotFoundException ex = assertThrows(
                OfferNotFoundException.class,
                () -> offerServiceCaller.callOfferService(url, testOfferId)
        );

        assertEquals("Offer not found for offerId=" + testOfferId, ex.getMessage());
        assertEquals(1, mockWebServer.getRequestCount() - requestCountBaseline);
    }

    @Test
    @Order(3)
    @DisplayName("200 response returns body directly on happy path")
    void callOfferService_whenServerReturns200_thenReturnsBody() {
        // given
        mockWebServer.enqueue(
                new MockResponse()
                        .setBody("owner@example.com")
                        .setResponseCode(200)
        );
        String url = "http://localhost:" + mockWebServer.getPort() + "/api/v1/offers/1/owner";

        // when
        String owner = offerServiceCaller.callOfferService(url, UUID.randomUUID());

        // then
        assertEquals("owner@example.com", owner);
        assertEquals(1, mockWebServer.getRequestCount() - requestCountBaseline);
    }

    @Test
    @Order(4)
    @DisplayName("A 4xx other than 404 is not retried: a bad-response failure surfaces, not a missing offer")
    void callOfferService_whenServerReturns4xxOtherThan404_thenThrowsBadResponseWithoutRetrying() {
        // given
        mockWebServer.enqueue(new MockResponse().setResponseCode(403));
        UUID testOfferId = UUID.fromString("00000000-0000-0000-0000-000000000077");
        String url = "http://localhost:" + mockWebServer.getPort() + "/api/v1/offers/77/owner";

        // when / then
        OfferServiceBadResponseException ex = assertThrows(
                OfferServiceBadResponseException.class,
                () -> offerServiceCaller.callOfferService(url, testOfferId)
        );

        assertEquals(OfferServiceBadResponseException.class, ex.getClass(),
                "a 403 must not be reported to the caller as a missing offer");
        assertEquals("Could not resolve offer owner for offerId=" + testOfferId, ex.getMessage());
        assertEquals(1, mockWebServer.getRequestCount() - requestCountBaseline);
    }

    @Test
    @Order(5)
    @DisplayName("An open circuit breaker fails fast through the fallback without reaching the offer service")
    void callOfferService_whenCircuitBreakerIsOpen_thenFallbackThrowsUnavailableAndNoRequestIsSent() {
        // given
        circuitBreakerRegistry.circuitBreaker("offerService").transitionToOpenState();
        UUID testOfferId = UUID.fromString("00000000-0000-0000-0000-000000000088");
        String url = "http://localhost:" + mockWebServer.getPort() + "/api/v1/offers/88/owner";

        // when / then
        OfferServiceUnavailableException ex = assertThrows(
                OfferServiceUnavailableException.class,
                () -> offerServiceCaller.callOfferService(url, testOfferId)
        );

        assertEquals("Offer service unavailable for offerId=" + testOfferId, ex.getMessage());
        assertThat(ex).hasCauseInstanceOf(CallNotPermittedException.class);
        assertEquals(0, mockWebServer.getRequestCount() - requestCountBaseline,
                "an open circuit breaker must not let the call reach sky-offer");
    }

    @Test
    @Order(6)
    @DisplayName("A refused connection is retried and surfaces as a transport failure, not as a rejected request")
    void callOfferService_whenConnectionIsRefused_thenRetriesAndThrowsResourceAccessException() {
        // given
        String url = "http://localhost:" + closedPort() + "/api/v1/offers/66/owner";

        // when / then
        ResourceAccessException ex = assertThrows(
                ResourceAccessException.class,
                () -> offerServiceCaller.callOfferService(url, UUID.randomUUID())
        );

        assertThat(ex).hasCauseInstanceOf(ConnectException.class);
        assertEquals(0, mockWebServer.getRequestCount() - requestCountBaseline);
    }

    @Test
    @Order(7)
    @DisplayName("A response that never arrives trips the read timeout on every attempt, max-attempts times")
    void callOfferService_whenResponseStalls_thenRetriesMaxAttemptsTimesAndThrowsResourceAccessException() {
        // given
        mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        String url = "http://localhost:" + mockWebServer.getPort() + "/api/v1/offers/55/owner";

        // when / then
        assertThrows(
                ResourceAccessException.class,
                () -> offerServiceCaller.callOfferService(url, UUID.randomUUID())
        );

        assertEquals(3, mockWebServer.getRequestCount() - requestCountBaseline);
    }

    @Test
    @Order(8)
    @DisplayName("A full window of 404s leaves the circuit closed, so a missing offer never becomes an outage")
    void callOfferService_whenEveryCallInTheWindowReturns404_thenCircuitStaysClosed() {
        // given
        for (int i = 0; i < SLIDING_WINDOW_SIZE; i++) {
            mockWebServer.enqueue(new MockResponse().setResponseCode(404));
        }
        String url = "http://localhost:" + mockWebServer.getPort() + "/api/v1/offers/44/owner";

        // when
        for (int i = 0; i < SLIDING_WINDOW_SIZE; i++) {
            assertThrows(
                    OfferNotFoundException.class,
                    () -> offerServiceCaller.callOfferService(url, UUID.randomUUID())
            );
        }

        // then
        assertEquals(CircuitBreaker.State.CLOSED,
                circuitBreakerRegistry.circuitBreaker("offerService").getState());
        assertEquals(SLIDING_WINDOW_SIZE, mockWebServer.getRequestCount() - requestCountBaseline);
    }

    @Test
    @Order(9)
    @DisplayName("A full window of transport failures still opens the circuit")
    void circuitBreaker_whenWindowFullOfTransportFailures_thenOpens() {
        // given
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("offerService");

        // when
        for (int i = 0; i < SLIDING_WINDOW_SIZE; i++) {
            circuitBreaker.onError(0, TimeUnit.MILLISECONDS, new ResourceAccessException("offer service unreachable"));
        }

        // then
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }

    private static int closedPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException ex) {
            throw new IllegalStateException("could not reserve a closed port", ex);
        }
    }
}
