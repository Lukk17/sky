package com.lukk.sky.booking.adapters.api;

import com.lukk.sky.booking.Assemblers.BookingAssembler;
import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.adapters.dto.BookingPayload;
import com.lukk.sky.booking.config.WebClientTestConfig;
import com.lukk.sky.booking.domain.model.Booking;
import com.lukk.sky.booking.domain.ports.repository.BookingRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;
import java.util.List;

import static com.lukk.sky.booking.Assemblers.UserAssembler.TEST_OWNER_EMAIL_2;
import static com.lukk.sky.booking.Assemblers.UserAssembler.TEST_USER_EMAIL;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions = 1, topics = {"bookingTopic-1"})
@Import(WebClientTestConfig.class)
public class BookingIntegrationTest {
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
    public void setUp() {
        consumer = consumerFactory.createConsumer("skyGroup", "0");
        consumer.subscribe(Collections.singletonList("bookingTopic-1"));
    }

    @AfterEach
    public void tearDown() {
        consumer.close();
        clearDatabase();
    }

    @Test
    public void testCreateBooking() throws InterruptedException {
//Given
        BookingPayload bookingPayload = BookingAssembler.getBookingPayload();

        mockWebServer.enqueue(new MockResponse().setBody(TEST_OWNER_EMAIL_2).setResponseCode(200));

        HttpHeaders headers = createTestHttpHeaders();
        HttpEntity<BookingPayload> request = new HttpEntity<>(bookingPayload, headers);
//When
        ResponseEntity<BookingDTO> actual = restTemplate.exchange(
                "/api/bookings",
                HttpMethod.POST,
                request,
                BookingDTO.class);

//Then
        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, BOOKING_TOPIC);

        assertEquals(HttpStatus.CREATED, actual.getStatusCode());

        assertKafkaPayload(requireNonNull(actual.getBody()), record);
        assertBookingFields(bookingPayload, actual.getBody());

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals(String.format("/owner/offer/%s", bookingPayload.offerId()), recordedRequest.getPath());
    }

    @Test
    public void testGetAllBookings() {
//Given
        List<Booking> bookings = populateDatabaseWithMany();
        HttpHeaders headers = createTestHttpHeaders();
        HttpEntity<?> request = new HttpEntity<>(headers);
//When
        ResponseEntity<BookingDTO[]> actual = restTemplate.exchange(
                "/api/user/bookings",
                HttpMethod.GET,
                request,
                BookingDTO[].class);

//Then
        assertEquals(HttpStatus.OK, actual.getStatusCode());

        for (int i = 0; i < bookings.size(); i++) {
            assertBookingFields(bookings.get(i), requireNonNull(actual.getBody())[i]);
        }
    }

    @Test
    public void testDeleteBooking() {
//Given
        Long bookingId = populateDatabase().getId();

        HttpHeaders headers = createTestHttpHeaders();
        HttpEntity<?> request = new HttpEntity<>(headers);
//When
        ResponseEntity<String> actual = restTemplate.exchange(
                "/api/bookings/" + bookingId,
                HttpMethod.DELETE,
                request,
                String.class);
//Then
        ResponseEntity<BookingDTO[]> savedBookings = restTemplate.exchange(
                "/api/user/bookings",
                HttpMethod.GET,
                request,
                BookingDTO[].class);

        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertTrue(requireNonNull(actual.getBody()).contains("Booking removed by user"));
        assertEquals(0, requireNonNull(savedBookings.getBody()).length);
    }

    private Booking populateDatabase() {
        return bookingRepository.save(BookingAssembler.getPopulatedBooked());
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
        headers.set("X-Forwarded-User", TEST_USER_EMAIL);
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
        assertTrue(record.value().contains(actual.getOfferId()));
        assertTrue(record.value().contains(actual.getOwnerEmail()));
    }
}
