package com.lukk.sky.booking.adapters.inbound.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lukk.sky.booking.AbstractIntegrationTest;
import com.lukk.sky.booking.Assemblers.BookingAssembler;
import com.lukk.sky.booking.TestSecurityConfig;
import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.adapters.dto.BookingPayload;
import com.lukk.sky.booking.config.WebClientTestConfig;
import com.lukk.sky.booking.domain.model.Booking;
import com.lukk.sky.booking.domain.ports.outbound.BookingRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.lukk.sky.booking.Assemblers.UserAssembler.TEST_OWNER_EMAIL_2;
import static com.lukk.sky.booking.Assemblers.UserAssembler.TEST_USER_EMAIL;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Booking API integration tests")
@Import({WebClientTestConfig.class, TestSecurityConfig.class})
class BookingIntegrationTest extends AbstractIntegrationTest {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TestPage<T>(List<T> content, long totalElements) {
    }

    public static final String BOOKING_TOPIC = "bookingTopic-1";

    @Autowired
    private MockWebServer mockWebServer;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ConsumerFactory<String, String> consumerFactory;

    private Consumer<String, String> consumer;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        consumer = consumerFactory.createConsumer("skyGroup-" + System.nanoTime(), "0");
        consumer.subscribe(Collections.singletonList(BOOKING_TOPIC));

        int attempts = 0;
        while (consumer.assignment().isEmpty() && attempts++ < 20) {
            consumer.poll(Duration.ofMillis(100));
        }
        consumer.seekToEnd(consumer.assignment());
        consumer.assignment().forEach(consumer::position);
    }

    @AfterEach
    void tearDown() {
        consumer.close();
        clearDatabase();
    }

    @Test
    @DisplayName("createBooking persists the booking, publishes a Kafka event, and calls the offer service")
    void createBooking_whenRequestIsValid_thenPersistAndPublishKafkaEvent() throws InterruptedException {
        // given
        BookingPayload bookingPayload = BookingAssembler.getBookingPayload();
        mockWebServer.enqueue(new MockResponse().setBody(TEST_OWNER_EMAIL_2).setResponseCode(200));
        HttpHeaders headers = createTestHttpHeaders();
        HttpEntity<BookingPayload> request = new HttpEntity<>(bookingPayload, headers);

        // when
        ResponseEntity<BookingDTO> actual = restTemplate.exchange(
                "/api/v1/bookings",
                HttpMethod.POST,
                request,
                BookingDTO.class);

        // then
        assertEquals(HttpStatus.CREATED, actual.getStatusCode());

        AtomicReference<ConsumerRecord<String, String>> recordRef = new AtomicReference<>();
        Awaitility.await()
                .atMost(20, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ConsumerRecord<String, String> record =
                            KafkaTestUtils.getSingleRecord(consumer, BOOKING_TOPIC, Duration.ofSeconds(1));
                    recordRef.set(record);
                });

        assertKafkaPayload(requireNonNull(actual.getBody()), recordRef.get());
        assertBookingFields(bookingPayload, actual.getBody());

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals(String.format("/api/v1/offers/%s/owner", bookingPayload.offerId()), recordedRequest.getPath());
    }

    @Test
    @DisplayName("getAllBookings returns all bookings for the user in paged response when bookings exist")
    void getAllBookings_whenBookingsExistInDatabase_thenReturnAllBookings() {
        // given
        List<Booking> bookings = populateDatabaseWithMany();
        HttpHeaders headers = createTestHttpHeaders();
        HttpEntity<?> request = new HttpEntity<>(headers);

        // when
        ResponseEntity<TestPage<BookingDTO>> actual = restTemplate.exchange(
                "/api/v1/user/bookings",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<TestPage<BookingDTO>>() {
                });

        // then
        assertEquals(HttpStatus.OK, actual.getStatusCode());
        List<BookingDTO> content = requireNonNull(actual.getBody()).content();
        assertEquals(bookings.size(), content.size());
        for (int i = 0; i < bookings.size(); i++) {
            assertBookingFields(bookings.get(i), content.get(i));
        }
    }

    @Test
    @DisplayName("deleteBooking removes the booking and returns 204 No Content when the booking exists")
    void deleteBooking_whenBookingExists_thenRemoveAndReturn204() {
        // given
        UUID bookingId = populateDatabase().getId();
        HttpHeaders headers = createTestHttpHeaders();
        HttpEntity<?> request = new HttpEntity<>(headers);

        // when
        ResponseEntity<Void> actual = restTemplate.exchange(
                "/api/v1/bookings/" + bookingId,
                HttpMethod.DELETE,
                request,
                Void.class);

        // then
        ResponseEntity<TestPage<BookingDTO>> savedBookings = restTemplate.exchange(
                "/api/v1/user/bookings",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<TestPage<BookingDTO>>() {
                });

        assertEquals(HttpStatus.NO_CONTENT, actual.getStatusCode());
        assertEquals(0, requireNonNull(savedBookings.getBody()).content().size());
    }

    private Booking populateDatabase() {
        Booking booking = BookingAssembler.getPopulatedBooked();
        booking.setId(null);
        return bookingRepository.save(booking);
    }

    private List<Booking> populateDatabaseWithMany() {
        return BookingAssembler.getPopulatedBookedList().stream()
                .peek(booking -> booking.setId(null))
                .map(booking -> bookingRepository.save(booking))
                .toList();
    }

    private void clearDatabase() {
        bookingRepository.deleteAll();
    }

    private static HttpHeaders createTestHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(TEST_USER_EMAIL.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        return headers;
    }

    private static void assertBookingFields(BookingPayload expected, BookingDTO actual) {
        assertEquals(expected.dateToBook(), actual.getBookedDate());
        assertEquals(expected.offerId(), actual.getOfferId());
        assertEquals(TEST_OWNER_EMAIL_2, actual.getOwnerEmail());
        assertEquals(TEST_USER_EMAIL, actual.getBookingUser());
    }

    private static void assertBookingFields(Booking expected, BookingDTO actual) {
        assertEquals(expected.getBookedDate().toString(), actual.getBookedDate());
        assertEquals(expected.getOfferId(), actual.getOfferId());
        assertEquals(expected.getOwnerEmail(), actual.getOwnerEmail());
        assertEquals(expected.getBookingUser(), actual.getBookingUser());
    }

    private static void assertKafkaPayload(BookingDTO actual, ConsumerRecord<String, String> record) {
        assertTrue(record.value().contains(actual.getBookedDate()));
        assertTrue(record.value().contains(actual.getBookingUser()));
        assertTrue(record.value().contains(actual.getOfferId().toString()));
        assertTrue(record.value().contains(actual.getOwnerEmail()));
    }
}
