package com.lukk.sky.offer.adapters.inbound.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lukk.sky.offer.AbstractIntegrationTest;
import com.lukk.sky.offer.assemblers.OfferAssembler;
import com.lukk.sky.offer.TestSecurityConfig;
import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.adapters.dto.OfferEditDTO;
import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.ports.outbound.OfferRepository;
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

import static com.lukk.sky.offer.assemblers.OfferAssembler.TEST_CITY;
import static com.lukk.sky.offer.assemblers.OfferAssembler.TEST_COMMENT;
import static com.lukk.sky.offer.assemblers.OfferAssembler.TEST_COUNTRY;
import static com.lukk.sky.offer.assemblers.OfferAssembler.TEST_DEFAULT_OFFER_ID;
import static com.lukk.sky.offer.assemblers.OfferAssembler.TEST_DESCRIPTION;
import static com.lukk.sky.offer.assemblers.OfferAssembler.TEST_HOTEL_NAME;
import static com.lukk.sky.offer.assemblers.OfferAssembler.TEST_EXTERNAL_PHOTO_URL;
import static com.lukk.sky.offer.assemblers.OfferAssembler.TEST_PRICE;
import static com.lukk.sky.offer.assemblers.OfferAssembler.TEST_ROOM_CAPACITY;
import static com.lukk.sky.offer.assemblers.OfferAssembler.getPopulatedOffer;
import static com.lukk.sky.offer.assemblers.UserAssembler.TEST_OWNER_EMAIL;
import static com.lukk.sky.offer.assemblers.UserAssembler.TEST_OWNER_EMAIL_2;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Offer Integration Tests: full HTTP stack with Testcontainers Kafka and PostgreSQL")
@Import(TestSecurityConfig.class)
class OfferIntegrationTest extends AbstractIntegrationTest {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TestPage<T>(List<T> content, long totalElements) {
    }

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
    void setUp() {
        consumer = consumerFactory.createConsumer("skyGroup-" + System.nanoTime(), "0");
        consumer.subscribe(Collections.singletonList(OFFER_TOPIC));

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
    @DisplayName("POST /api/v1/owner/offers creates the offer, returns 201, and publishes a Kafka event")
    void createOffer_whenOwnerPostsNewOffer_thenReturn201AndPublishKafkaEvent() {
        // given
        OfferDTO offer = OfferDTO.of(OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID));
        offer.setId(null);
        HttpHeaders headers = createTestHttpHeaders();
        HttpEntity<OfferDTO> request = new HttpEntity<>(offer, headers);

        // when
        ResponseEntity<OfferDTO> actual = restTemplate.postForEntity("/api/v1/owner/offers", request, OfferDTO.class);
        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, OFFER_TOPIC, Duration.ofSeconds(20));

        // then
        assertEquals(HttpStatus.CREATED, actual.getStatusCode());
        assertOfferFields(requireNonNull(actual.getBody()), TEST_HOTEL_NAME);
        assertKafkaPayload(actual.getBody(), record);
    }

    @Test
    @DisplayName("GET /api/v1/owner/offers returns only the authenticated owner's offers in the page content")
    void getOwnedOffers_whenOwnerHasMultipleOffers_thenReturnOnlyOwnersOffers() {
        // given
        populateDatabase();
        Offer offer = getPopulatedOffer(UUID.randomUUID());
        offer.setId(null);
        offer.setOwnerEmail(TEST_OWNER_EMAIL_2);
        offerRepository.save(offer);
        HttpHeaders headers = createTestHttpHeaders();
        HttpEntity<?> request = new HttpEntity<>(headers);

        // when
        ResponseEntity<TestPage<OfferDTO>> actual = restTemplate.exchange(
                "/api/v1/owner/offers",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<TestPage<OfferDTO>>() {
                }
        );

        // then
        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertEquals(2, requireNonNull(actual.getBody()).content().size());
        assertEquals(2, actual.getBody().totalElements());
    }

    @Test
    @DisplayName("GET /api/v1/offers returns all offers in the page content")
    void getAllOffers_whenOffersExist_thenReturnAllOffers() {
        // given
        populateDatabase();

        // when
        ResponseEntity<TestPage<OfferDTO>> actual = restTemplate.exchange(
                "/api/v1/offers",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<TestPage<OfferDTO>>() {
                }
        );

        // then
        assertEquals(HttpStatus.OK, actual.getStatusCode());
        List<OfferDTO> offers = requireNonNull(actual.getBody()).content();
        OfferDTO firstOffer = offers.get(0);
        assertOfferFields(firstOffer, TEST_HOTEL_NAME);
    }

    @Test
    @DisplayName("GET /api/v1/offers/{id}/owner returns the owner email for an existing offer")
    void getOfferOwner_whenOfferExists_thenReturnOwnerEmail() {
        // given
        UUID offerId = populateDatabase().getId();
        HttpHeaders headers = createTestHttpHeaders();
        HttpEntity<?> request = new HttpEntity<>(headers);

        // when
        ResponseEntity<String> actual = restTemplate.exchange(
                "/api/v1/offers/" + offerId + "/owner",
                HttpMethod.GET,
                request,
                String.class);

        // then
        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertEquals(TEST_OWNER_EMAIL, actual.getBody());
    }

    @Test
    @DisplayName("PUT /api/v1/owner/offers updates the offer, returns 200, and publishes a Kafka event")
    void updateOffer_whenOwnerEditsOffer_thenReturn200AndPublishKafkaEvent() {
        // given
        UUID offerId = populateDatabase().getId();
        OfferEditDTO updatedOffer = OfferEditDTO.of(OfferAssembler.getPopulatedOffer(offerId));
        updatedOffer.setHotelName(UPDATED_NAME);
        HttpHeaders headers = createTestHttpHeaders();
        HttpEntity<OfferEditDTO> request = new HttpEntity<>(updatedOffer, headers);

        // when
        ResponseEntity<OfferDTO> actual = restTemplate.exchange(
                "/api/v1/owner/offers",
                HttpMethod.PUT,
                request,
                OfferDTO.class);
        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, OFFER_TOPIC, Duration.ofSeconds(20));

        // then
        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertOfferFields(requireNonNull(actual.getBody()), UPDATED_NAME);
        assertKafkaPayload(actual.getBody(), record);
    }

    @Test
    @DisplayName("DELETE /api/v1/owner/offers/{id} removes the offer and publishes a Kafka event")
    void deleteOffer_whenOwnerDeletesOffer_thenOfferRemovedAndKafkaEventPublished() {
        // given
        UUID offerId = populateDatabase().getId();
        HttpHeaders headers = createTestHttpHeaders();
        HttpEntity<?> request = new HttpEntity<>(headers);

        // when
        restTemplate.exchange(
                "/api/v1/owner/offers/" + offerId,
                HttpMethod.DELETE,
                request,
                String.class);

        // then
        ResponseEntity<TestPage<OfferDTO>> savedOffersPage = restTemplate.exchange(
                "/api/v1/offers",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<TestPage<OfferDTO>>() {
                }
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

    @Test
    @DisplayName("POST /api/v1/owner/offers ignores a client-supplied id and assigns a server-generated one")
    void createOffer_whenClientSuppliesAnId_thenIgnoreItAndAssignAServerGeneratedOne() {
        // given
        UUID clientChosenId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        OfferDTO offer = OfferDTO.of(OfferAssembler.getPopulatedOffer(clientChosenId));
        HttpHeaders headers = createTestHttpHeaders();
        HttpEntity<OfferDTO> request = new HttpEntity<>(offer, headers);

        // when
        ResponseEntity<OfferDTO> actual = restTemplate.postForEntity("/api/v1/owner/offers", request, OfferDTO.class);

        // then
        assertEquals(HttpStatus.CREATED, actual.getStatusCode());
        assertNotEquals(clientChosenId, requireNonNull(actual.getBody()).getId());
        assertTrue(offerRepository.findById(clientChosenId).isEmpty(),
                "the identifier a caller asked for must never reach the database");
    }

    @Test
    @DisplayName("POST /api/v1/owner/offers with a garbage ownerEmail succeeds, because the server assigns it from the JWT")
    void createOffer_whenOwnerEmailIsGarbage_thenIgnoreItAndUseTheJwtEmail() {
        // given
        OfferDTO offer = OfferDTO.of(OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID));
        offer.setId(null);
        offer.setOwnerEmail("not-an-email");
        HttpHeaders headers = createTestHttpHeaders();
        HttpEntity<OfferDTO> request = new HttpEntity<>(offer, headers);

        // when
        ResponseEntity<OfferDTO> actual = restTemplate.postForEntity("/api/v1/owner/offers", request, OfferDTO.class);

        // then
        assertEquals(HttpStatus.CREATED, actual.getStatusCode());
        assertEquals(TEST_OWNER_EMAIL, requireNonNull(actual.getBody()).getOwnerEmail());
    }

    @Test
    @DisplayName("PUT /api/v1/owner/offers with a blank hotelName returns 400, never a 500 from the entity constraint")
    void updateOffer_whenHotelNameIsBlank_thenReturn400() {
        // given
        UUID offerId = populateDatabase().getId();
        OfferEditDTO updatedOffer = OfferAssembler.getPopulatedOfferEditDTO(offerId);
        updatedOffer.setHotelName("   ");
        HttpHeaders headers = createTestHttpHeaders();
        HttpEntity<OfferEditDTO> request = new HttpEntity<>(updatedOffer, headers);

        // when
        ResponseEntity<String> actual = restTemplate.exchange(
                "/api/v1/owner/offers",
                HttpMethod.PUT,
                request,
                String.class);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, actual.getStatusCode());
        assertTrue(requireNonNull(actual.getBody()).contains("field-errors"));
        assertTrue(actual.getBody().contains("hotelName"));
    }

    @Test
    @DisplayName("PUT /api/v1/owner/offers by a user who does not own the offer returns 403 and leaves the owner unchanged")
    void updateOffer_whenCallerDoesNotOwnTheOffer_thenReturn403() {
        // given
        UUID offerId = populateDatabase().getId();
        OfferEditDTO updatedOffer = OfferAssembler.getPopulatedOfferEditDTO(offerId);
        updatedOffer.setHotelName(UPDATED_NAME);
        HttpHeaders headers = createTestHttpHeaders(TEST_OWNER_EMAIL_2);
        HttpEntity<OfferEditDTO> request = new HttpEntity<>(updatedOffer, headers);

        // when
        ResponseEntity<String> actual = restTemplate.exchange(
                "/api/v1/owner/offers",
                HttpMethod.PUT,
                request,
                String.class);

        // then
        assertEquals(HttpStatus.FORBIDDEN, actual.getStatusCode());
        Offer unchanged = offerRepository.findById(offerId).orElseThrow();
        assertEquals(TEST_OWNER_EMAIL, unchanged.getOwnerEmail(),
                "an edit by a non-owner must never transfer ownership");
        assertEquals(TEST_HOTEL_NAME, unchanged.getHotelName());
    }

    @Test
    @DisplayName("DELETE /api/v1/owner/offers/{id} by a user who does not own the offer returns 403, not 400")
    void deleteOffer_whenCallerDoesNotOwnTheOffer_thenReturn403() {
        // given
        UUID offerId = populateDatabase().getId();
        HttpHeaders headers = createTestHttpHeaders(TEST_OWNER_EMAIL_2);
        HttpEntity<?> request = new HttpEntity<>(headers);

        // when
        ResponseEntity<String> actual = restTemplate.exchange(
                "/api/v1/owner/offers/" + offerId,
                HttpMethod.DELETE,
                request,
                String.class);

        // then
        assertEquals(HttpStatus.FORBIDDEN, actual.getStatusCode());
        assertTrue(offerRepository.findById(offerId).isPresent());
    }

    @Test
    @DisplayName("GET /api/v1/offers with an unknown sort property returns 400 naming it, not a 500")
    void getAllOffers_whenSortPropertyIsUnknown_thenReturn400() {
        // when
        ResponseEntity<String> actual = restTemplate.exchange(
                "/api/v1/offers?sort=hotelNamee",
                HttpMethod.GET,
                null,
                String.class);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, actual.getStatusCode());
        assertTrue(requireNonNull(actual.getBody()).contains("hotelNamee"));
    }

    private static HttpHeaders createTestHttpHeaders() {
        return createTestHttpHeaders(TEST_OWNER_EMAIL);
    }

    private static HttpHeaders createTestHttpHeaders(String user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(user.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        return headers;
    }

    private Offer populateDatabase() {
        Offer offer = getPopulatedOffer(UUID.randomUUID());
        offer.setId(null);
        offer.setPhotoObjectKey(null);
        offerRepository.save(offer);

        Offer offer1 = getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        offer1.setId(null);
        offer1.setPhotoObjectKey(null);
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
        assertEquals(TEST_EXTERNAL_PHOTO_URL, actual.getExternalPhotoUrl());
        assertEquals(TEST_EXTERNAL_PHOTO_URL, actual.getPhotoUrl());
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
        assertTrue(record.value().contains(actual.getExternalPhotoUrl()));
        assertTrue(record.value().contains(actual.getId().toString()));
        assertTrue(record.value().contains(actual.getRoomCapacity().toString()));
        assertTrue(record.value().contains(actual.getPrice().toString()));
    }

    @Test
    @DisplayName("PUT /api/v1/owner/offers cannot move the stored photo, whatever the payload carries")
    void updateOffer_whenPayloadCarriesAnotherPhotoAddress_thenTheStoredObjectKeySurvives() {
        // given
        Offer stored = populateDatabase();
        String storedKey = "offers/" + stored.getId() + "/11111111-1111-4111-8111-111111111111-hotel.png";
        stored.setPhotoObjectKey(storedKey);
        offerRepository.save(stored);
        OfferEditDTO updatedOffer = OfferEditDTO.of(stored);
        updatedOffer.setHotelName(UPDATED_NAME);
        updatedOffer.setExternalPhotoUrl("https://images.example.com/somewhere-else.jpeg");
        HttpEntity<OfferEditDTO> request = new HttpEntity<>(updatedOffer, createTestHttpHeaders());

        // when
        ResponseEntity<OfferDTO> actual = restTemplate.exchange(
                "/api/v1/owner/offers",
                HttpMethod.PUT,
                request,
                OfferDTO.class);

        // then
        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertEquals(storedKey, offerRepository.findById(stored.getId()).orElseThrow().getPhotoObjectKey());
        assertTrue(requireNonNull(actual.getBody()).getPhotoUrl().contains(storedKey));
    }

    @Test
    @DisplayName("PUT /api/v1/owner/offers rejects an external photo address that is not an absolute URL")
    void updateOffer_whenExternalPhotoUrlIsNotAbsolute_thenReturn400() {
        // given
        Offer stored = populateDatabase();
        OfferEditDTO updatedOffer = OfferEditDTO.of(stored);
        updatedOffer.setExternalPhotoUrl("/images/grand-hotel-paris-new.jpg");
        HttpEntity<OfferEditDTO> request = new HttpEntity<>(updatedOffer, createTestHttpHeaders());

        // when
        ResponseEntity<String> actual = restTemplate.exchange(
                "/api/v1/owner/offers",
                HttpMethod.PUT,
                request,
                String.class);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, actual.getStatusCode());
        assertEquals(TEST_EXTERNAL_PHOTO_URL,
                offerRepository.findById(stored.getId()).orElseThrow().getExternalPhotoUrl());
    }

    @Test
    @DisplayName("DELETE /api/v1/owner/offers/{offerId}/photo clears the stored key and returns 204")
    void deletePhoto_whenOwnerDeletesPhoto_thenReturn204AndClearTheStoredKey() {
        // given
        Offer stored = populateDatabase();
        stored.setPhotoObjectKey("offers/" + stored.getId() + "/11111111-1111-4111-8111-111111111111-hotel.png");
        offerRepository.save(stored);
        HttpEntity<?> request = new HttpEntity<>(createTestHttpHeaders());

        // when
        ResponseEntity<Void> actual = restTemplate.exchange(
                "/api/v1/owner/offers/" + stored.getId() + "/photo",
                HttpMethod.DELETE,
                request,
                Void.class);

        // then
        assertEquals(HttpStatus.NO_CONTENT, actual.getStatusCode());
        assertNull(offerRepository.findById(stored.getId()).orElseThrow().getPhotoObjectKey());
    }

    @Test
    @DisplayName("DELETE /api/v1/owner/offers/{offerId}/photo answers 403 for a caller who does not own the offer")
    void deletePhoto_whenCallerIsNotOwner_thenReturn403AndKeepTheStoredKey() {
        // given
        Offer stored = populateDatabase();
        String storedKey = "offers/" + stored.getId() + "/11111111-1111-4111-8111-111111111111-hotel.png";
        stored.setPhotoObjectKey(storedKey);
        offerRepository.save(stored);
        HttpEntity<?> request = new HttpEntity<>(createTestHttpHeaders(TEST_OWNER_EMAIL_2));

        // when
        ResponseEntity<String> actual = restTemplate.exchange(
                "/api/v1/owner/offers/" + stored.getId() + "/photo",
                HttpMethod.DELETE,
                request,
                String.class);

        // then
        assertEquals(HttpStatus.FORBIDDEN, actual.getStatusCode());
        assertEquals(storedKey, offerRepository.findById(stored.getId()).orElseThrow().getPhotoObjectKey());
    }
}
