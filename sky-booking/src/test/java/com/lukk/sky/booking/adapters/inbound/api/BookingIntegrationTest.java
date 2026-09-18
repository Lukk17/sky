package com.lukk.sky.booking.adapters.inbound.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lukk.sky.booking.AbstractIntegrationTest;
import com.lukk.sky.booking.assemblers.BookingAssembler;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.lukk.sky.booking.assemblers.UserAssembler.TEST_OWNER_EMAIL_2;
import static com.lukk.sky.booking.assemblers.UserAssembler.TEST_USER_EMAIL;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Autowired
    private ObjectMapper objectMapper;

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
    @DisplayName("deleteBooking removes the booking, returns 204 No Content, and publishes a Kafka event")
    void deleteBooking_whenBookingExists_thenRemoveAndReturn204AndPublishKafkaEvent() {
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

        ConsumerRecord<String, String> record =
                KafkaTestUtils.getSingleRecord(consumer, BOOKING_TOPIC, Duration.ofSeconds(20));
        JsonNode envelope = objectMapper.readTree(record.value());
        assertEquals(TEST_USER_EMAIL, envelope.get("userInfo").asString());
        assertEquals("Booking removed by user", envelope.get("payload").asString());
    }

    @Test
    @DisplayName("createBooking with a dateToBook that is not a date returns 400, never a 500 from the parser")
    void createBooking_whenDateToBookIsNotADate_thenReturn400() {
        // given
        mockWebServer.enqueue(new MockResponse().setBody(TEST_OWNER_EMAIL_2).setResponseCode(200));
        int requestsBefore = mockWebServer.getRequestCount();
        HttpHeaders headers = createTestHttpHeaders(TEST_USER_EMAIL);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(
                "{\"offerId\":\"" + BookingAssembler.TEST_DEFAULT_OFFER_ID + "\",\"dateToBook\":\"tomorrow\"}",
                headers);

        // when
        ResponseEntity<String> actual = restTemplate.exchange(
                "/api/v1/bookings",
                HttpMethod.POST,
                request,
                String.class);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, actual.getStatusCode());
        assertEquals(0, bookingRepository.count());
        assertEquals(requestsBefore, mockWebServer.getRequestCount(),
                "an unparseable date is rejected at the boundary, before sky-offer is called");
        assertTrue(requireNonNull(actual.getBody()).contains("Failed to read request"),
                "the published contract documents this detail string for an unreadable body");
    }

    @Test
    @DisplayName("createBooking for an offer sky-offer does not have returns 404, distinguishable from a rejected booking")
    void createBooking_whenOfferDoesNotExist_thenReturn404() {
        // given
        BookingPayload bookingPayload = BookingAssembler.getBookingPayload();
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));
        HttpHeaders headers = createTestHttpHeaders(TEST_USER_EMAIL);
        HttpEntity<BookingPayload> request = new HttpEntity<>(bookingPayload, headers);

        // when
        ResponseEntity<String> actual = restTemplate.exchange(
                "/api/v1/bookings",
                HttpMethod.POST,
                request,
                String.class);

        // then
        assertEquals(HttpStatus.NOT_FOUND, actual.getStatusCode());
        assertEquals(0, bookingRepository.count());
    }

    @Test
    @DisplayName("deleteBooking by a user who neither booked nor owns the offer returns 403, not 400")
    void deleteBooking_whenCallerIsNeitherBookerNorOwner_thenReturn403() {
        // given
        UUID bookingId = populateDatabase().getId();
        HttpHeaders headers = createTestHttpHeaders("stranger@sky.dev");
        HttpEntity<?> request = new HttpEntity<>(headers);

        // when
        ResponseEntity<String> actual = restTemplate.exchange(
                "/api/v1/bookings/" + bookingId,
                HttpMethod.DELETE,
                request,
                String.class);

        // then
        assertEquals(HttpStatus.FORBIDDEN, actual.getStatusCode());
        assertEquals(1, bookingRepository.count());
        assertFalse(requireNonNull(actual.getBody()).contains("stranger@sky.dev"),
                "the caller's email is personal data and stays out of the body");
    }

    @Test
    @DisplayName("createBooking with a dateToBook in the past returns 400 and persists nothing")
    void createBooking_whenDateToBookIsInThePast_thenReturn400AndPersistNothing() {
        // given
        BookingPayload bookingPayload =
                new BookingPayload(BookingAssembler.TEST_DEFAULT_OFFER_ID, LocalDate.now().minusDays(1));
        mockWebServer.enqueue(new MockResponse().setBody(TEST_OWNER_EMAIL_2).setResponseCode(200));
        HttpHeaders headers = createTestHttpHeaders(TEST_USER_EMAIL);
        HttpEntity<BookingPayload> request = new HttpEntity<>(bookingPayload, headers);

        // when
        ResponseEntity<String> actual = restTemplate.exchange(
                "/api/v1/bookings",
                HttpMethod.POST,
                request,
                String.class);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, actual.getStatusCode());
        assertEquals(0, bookingRepository.count());
        assertTrue(requireNonNull(actual.getBody()).contains("You try to book offer with date in the past."),
                "the rejected booking explains why the date was refused");
    }

    @Test
    @DisplayName("deleteBooking for an unknown booking id returns 404 naming the id, not 403 or 400")
    void deleteBooking_whenBookingDoesNotExist_thenReturn404() {
        // given
        UUID unknownBookingId = UUID.fromString("00000000-0000-0000-0000-0000000000ff");
        HttpHeaders headers = createTestHttpHeaders(TEST_USER_EMAIL);
        HttpEntity<?> request = new HttpEntity<>(headers);

        // when
        ResponseEntity<String> actual = restTemplate.exchange(
                "/api/v1/bookings/" + unknownBookingId,
                HttpMethod.DELETE,
                request,
                String.class);

        // then
        assertEquals(HttpStatus.NOT_FOUND, actual.getStatusCode());
        assertTrue(requireNonNull(actual.getBody())
                        .contains(String.format("No booking with ID: %s found.", unknownBookingId)),
                "the 404 body names the booking that could not be found");
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
        return createTestHttpHeaders(TEST_USER_EMAIL);
    }

    private static HttpHeaders createTestHttpHeaders(String user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(user.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        return headers;
    }

    private static void assertBookingFields(BookingPayload expected, BookingDTO actual) {
        assertEquals(expected.dateToBook().toString(), actual.getBookedDate());
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

    private void assertKafkaPayload(BookingDTO expected, ConsumerRecord<String, String> record) {
        JsonNode envelope = objectMapper.readTree(record.value());
        assertEquals(expected.getBookingUser(), envelope.get("userInfo").asString());

        JsonNode payload = objectMapper.readTree(envelope.get("payload").asString());
        assertEquals(5, payload.size());
        assertEquals(expected.getId().toString(), payload.get("id").asString());
        assertEquals(expected.getOfferId().toString(), payload.get("offerId").asString());
        assertEquals(expected.getBookedDate(), payload.get("bookedDate").asString());
        assertEquals(expected.getBookingUser(), payload.get("bookingUser").asString());
        assertEquals(expected.getOwnerEmail(), payload.get("ownerEmail").asString());
    }
}
