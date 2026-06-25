package com.lukk.sky.offer.adapters.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lukk.sky.offer.AbstractIntegrationTest;
import com.lukk.sky.offer.Assemblers.OfferAssembler;
import com.lukk.sky.offer.TestSecurityConfig;
import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.adapters.dto.OfferEditDTO;
import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.ports.repository.OfferRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static com.lukk.sky.offer.Assemblers.OfferAssembler.*;
import static com.lukk.sky.offer.Assemblers.UserAssembler.TEST_OWNER_EMAIL;
import static com.lukk.sky.offer.Assemblers.UserAssembler.TEST_OWNER_EMAIL_2;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Offer Integration Tests — full HTTP stack with embedded Kafka and H2 database")
@Import(TestSecurityConfig.class)
public class OfferIntegrationTest extends AbstractIntegrationTest {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TestPage<T>(List<T> content, long totalElements) {}

    public static final String UPDATED_NAME = "UpdatedName";
    public static final String OFFER_TOPIC = "offerTopic-1";
    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private ConsumerFactory<String, String> consumerFactory;

    private Consumer<String, String> consumer;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    public void setUp() {
        // Unique group per test, then seek to the end of the partition once assigned, so each
        // test consumes only the record it produces — never a record left on the shared
        // offerTopic-1 by another test (JUnit order is undefined) or by a reused broker.
        consumer = consumerFactory.createConsumer("skyGroup-" + System.nanoTime(), "0");
        consumer.subscribe(Collections.singletonList(OFFER_TOPIC));

        int attempts = 0;
        while (consumer.assignment().isEmpty() && attempts++ < 20) {
            consumer.poll(Duration.ofMillis(100));
        }
        consumer.seekToEnd(consumer.assignment());
        // seekToEnd is lazy — it resolves the end offset on the next poll, which would land
        // AFTER the record this test is about to produce. Force resolution to the current end
        // now, so getSingleRecord reads exactly the record produced by this test.
        consumer.assignment().forEach(consumer::position);
    }

    @AfterEach
    public void tearDown() {
        consumer.close();
        clearDatabase();
    }

    @Test
    @DisplayName("POST /api/v1/owner/offers creates the offer, returns 201, and publishes a Kafka event")
    public void createOffer_whenOwnerPostsNewOffer_thenReturn201AndPublishKafkaEvent() {
//Given
        OfferDTO offer = OfferDTO.of(OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID));
        // Let the server assign the id. Posting a hardcoded id sends save() down the merge
        // path for a detached entity that doesn't exist, so the create fails and never emits
        // its Kafka event (same reason populateDatabase() nulls ids before saving).
        offer.setId(null);

        HttpHeaders headers = createTestHttpHeaders();
        HttpEntity<OfferDTO> request = new HttpEntity<>(offer, headers);
//When
        ResponseEntity<OfferDTO> actual = restTemplate.postForEntity("/api/v1/owner/offers", request, OfferDTO.class);
//Then
        assertEquals(HttpStatus.CREATED, actual.getStatusCode());
        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, OFFER_TOPIC, Duration.ofSeconds(20));

        assertOfferFields(requireNonNull(actual.getBody()), TEST_HOTEL_NAME);
        assertKafkaPayload(actual.getBody(), record);
    }

    @Test
    @DisplayName("GET /api/v1/owner/offers returns only the authenticated owner's offers in the page content")
    public void getOwnedOffers_whenOwnerHasMultipleOffers_thenReturnOnlyOwnersOffers() {
//Given
        populateDatabase();

        Offer offer = getPopulatedOffer(2L);
        offer.setId(null);
        offer.setOwnerEmail(TEST_OWNER_EMAIL_2);
        offerRepository.save(offer);

        HttpHeaders headers = createTestHttpHeaders();
        HttpEntity<?> request = new HttpEntity<>(headers);
//When
        ResponseEntity<TestPage<OfferDTO>> actual = restTemplate.exchange(
                "/api/v1/owner/offers",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<TestPage<OfferDTO>>() {}
        );
//Then
        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertEquals(2, requireNonNull(actual.getBody()).content().size());
        assertEquals(2, actual.getBody().totalElements());
    }

    @Test
    @DisplayName("GET /api/v1/offers returns all offers in the page content")
    public void getAllOffers_whenOffersExist_thenReturnAllOffers() {
//Given
        populateDatabase();
//When
        ResponseEntity<TestPage<OfferDTO>> actual = restTemplate.exchange(
                "/api/v1/offers",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<TestPage<OfferDTO>>() {}
        );
//Then
        assertEquals(HttpStatus.OK, actual.getStatusCode());

        List<OfferDTO> offers = requireNonNull(actual.getBody()).content();
        OfferDTO firstOffer = offers.get(0);
        assertOfferFields(firstOffer, TEST_HOTEL_NAME);
    }

    @Test
    @DisplayName("GET /api/internal/owner/offer/{id} returns the owner email for an existing offer")
    public void getOfferOwner_whenOfferExists_thenReturnOwnerEmail() {
//Given
        long offerId = populateDatabase().getId();

        HttpHeaders headers = createTestHttpHeaders();
        HttpEntity<?> request = new HttpEntity<>(headers);
//When
        ResponseEntity<String> actual = restTemplate.exchange(
                "/api/internal/owner/offer/" + offerId,
                HttpMethod.GET,
                request,
                String.class);
//Then
        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertEquals(TEST_OWNER_EMAIL, actual.getBody());
    }

    @Test
    @DisplayName("PUT /api/v1/owner/offers updates the offer, returns 200, and publishes a Kafka event")
    public void updateOffer_whenOwnerEditsOffer_thenReturn200AndPublishKafkaEvent() {
//Given
        long offerId = populateDatabase().getId();

        OfferEditDTO updatedOffer = OfferEditDTO.of(OfferAssembler.getPopulatedOffer(offerId));
        updatedOffer.setHotelName(UPDATED_NAME);

        HttpHeaders headers = createTestHttpHeaders();
        HttpEntity<OfferEditDTO> request = new HttpEntity<>(updatedOffer, headers);
//When
        ResponseEntity<OfferDTO> actual = restTemplate.exchange(
                "/api/v1/owner/offers",
                HttpMethod.PUT,
                request,
                OfferDTO.class);
//Then
        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, OFFER_TOPIC, Duration.ofSeconds(20));

        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertOfferFields(requireNonNull(actual.getBody()), UPDATED_NAME);
        assertKafkaPayload(actual.getBody(), record);
    }

    @Test
    @DisplayName("DELETE /api/v1/owner/offers/{id} removes the offer and publishes a Kafka event")
    public void deleteOffer_whenOwnerDeletesOffer_thenOfferRemovedAndKafkaEventPublished() {
//Given
        long offerId = populateDatabase().getId();

        HttpHeaders headers = createTestHttpHeaders();
        HttpEntity<?> request = new HttpEntity<>(headers);
//When
        restTemplate.exchange(
                "/api/v1/owner/offers/" + offerId,
                HttpMethod.DELETE,
                request,
                String.class);
//Then
        ResponseEntity<TestPage<OfferDTO>> savedOffersPage = restTemplate.exchange(
                "/api/v1/offers",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<TestPage<OfferDTO>>() {}
        );

        List<OfferDTO> offers = requireNonNull(savedOffersPage.getBody()).content();

        assertTrue(offers
                .stream()
                .filter(offerDTO -> offerDTO.getId().equals(offerId))
                .findFirst()
                .isEmpty());

        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, OFFER_TOPIC, Duration.ofSeconds(20));

        assertTrue(record.value().contains("Offer with ID"));
        assertTrue(record.value().contains("was deleted"));
    }

    private static HttpHeaders createTestHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        // base64url so the stub JwtDecoder (TestSecurityConfig) decodes it back to the email claim;
        // a raw email contains '@', which is outside the RFC 6750 Bearer-token charset.
        headers.setBearerAuth(java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(TEST_OWNER_EMAIL.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        return headers;
    }

    private Offer populateDatabase() {
        // Null the assembler-hardcoded ids so save() takes the INSERT path and
        // Hibernate picks up the IDENTITY-assigned values rather than treating
        // a fixed id as a detached entity to merge.
        Offer offer = getPopulatedOffer(2L);
        offer.setId(null);
        offerRepository.save(offer);

        Offer offer1 = getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        offer1.setId(null);
        return offerRepository.save(offer1);
    }

    private void clearDatabase() {
        offerRepository.deleteAll();
    }

    private void assertOfferFields(OfferDTO actual, String hotelName) {
        int equal = 0;
        assertEquals(hotelName, actual.getHotelName());
        assertEquals(TEST_OWNER_EMAIL, actual.getOwnerEmail());
        assertEquals(TEST_CITY, actual.getCity());
        assertEquals(TEST_COMMENT, actual.getComment());
        assertEquals(TEST_COUNTRY, actual.getCountry());
        assertEquals(TEST_DESCRIPTION, actual.getDescription());
        assertEquals(TEST_PHOTO_PATH, actual.getPhotoPath());
        assertEquals(TEST_ROOM_CAPACITY, actual.getRoomCapacity());
        assertEquals(equal, TEST_PRICE.compareTo(actual.getPrice()));
    }

    private static void assertKafkaPayload(OfferDTO actual, ConsumerRecord<String, String> record) {
        assertTrue(record.value().contains(actual.getHotelName()));
        assertTrue(record.value().contains(actual.getOwnerEmail()));
        assertTrue(record.value().contains(actual.getCity()));
        assertTrue(record.value().contains(actual.getComment()));
        assertTrue(record.value().contains(actual.getCountry()));
        assertTrue(record.value().contains(actual.getDescription()));
        assertTrue(record.value().contains(actual.getPhotoPath()));
        assertTrue(record.value().contains(actual.getId().toString()));
        assertTrue(record.value().contains(actual.getRoomCapacity().toString()));
        assertTrue(record.value().contains(actual.getPrice().toString()));
    }
}
