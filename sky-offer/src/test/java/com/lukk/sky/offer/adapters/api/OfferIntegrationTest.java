package com.lukk.sky.offer.adapters.api;

import com.lukk.sky.offer.Assemblers.OfferAssembler;
import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.adapters.dto.OfferEditDTO;
import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.ports.repository.OfferRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.lukk.sky.offer.Assemblers.OfferAssembler.*;
import static com.lukk.sky.offer.Assemblers.UserAssembler.TEST_OWNER_EMAIL;
import static com.lukk.sky.offer.Assemblers.UserAssembler.TEST_OWNER_EMAIL_2;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions = 1, topics = {"offerTopic-1"})
public class OfferIntegrationTest {

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
        consumer = consumerFactory.createConsumer("skyGroup", "0");
        consumer.subscribe(Collections.singletonList("offerTopic-1"));
    }

    @AfterEach
    public void tearDown() {
        consumer.close();
        clearDatabase();
    }

    @Test
    public void testCreateOffer() {
//Given
        OfferDTO offer = OfferDTO.of(OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID));

        HttpHeaders headers = createTestHttpHeaders();
        HttpEntity<OfferDTO> request = new HttpEntity<>(offer, headers);
//When
        ResponseEntity<OfferDTO> actual = restTemplate.postForEntity("/api/owner/offer", request, OfferDTO.class);
//Then
        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, OFFER_TOPIC);

        assertOfferFields(requireNonNull(actual.getBody()), TEST_HOTEL_NAME);
        assertKafkaPayload(actual.getBody(), record);
    }

    @Test
    public void testGetAllOwnersOffers() {
//Given
        populateDatabase();

        Offer offer = getPopulatedOffer(2L);
        offer.setOwnerEmail(TEST_OWNER_EMAIL_2);
        offerRepository.save(offer);

        HttpHeaders headers = createTestHttpHeaders();
        HttpEntity<?> request = new HttpEntity<>(headers);
//When
        ResponseEntity<OfferDTO[]> actual = restTemplate.exchange(
                "/api/owner/offers",
                HttpMethod.GET,
                request,
                OfferDTO[].class
        );
//Then
        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertEquals(2, requireNonNull(actual.getBody()).length);
    }

    @Test
    public void testGetAllOffers() {
//Given
        populateDatabase();
//When
        ResponseEntity<OfferDTO[]> actual = restTemplate.getForEntity("/api/offers", OfferDTO[].class);
//Then
        assertEquals(HttpStatus.OK, actual.getStatusCode());

        List<OfferDTO> offers = Arrays.asList(requireNonNull(actual.getBody()));

        OfferDTO firstOffer = offers.get(0);
        assertOfferFields(firstOffer, TEST_HOTEL_NAME);
    }

    @Test
    public void testGetOfferOwner() {
//Given
        long offerId = populateDatabase().getId();

        HttpHeaders headers = createTestHttpHeaders();
        HttpEntity<?> request = new HttpEntity<>(headers);
//When
        ResponseEntity<String> actual = restTemplate.exchange(
                "/owner/offer/" + offerId,
                HttpMethod.GET,
                request,
                String.class);
//Then
        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertEquals(TEST_OWNER_EMAIL, actual.getBody());
    }

    @Test
    public void testUpdateOffer() {
//Given
        long offerId = populateDatabase().getId();

        OfferEditDTO updatedOffer = OfferEditDTO.of(OfferAssembler.getPopulatedOffer(offerId));
        updatedOffer.setHotelName(UPDATED_NAME);

        HttpHeaders headers = createTestHttpHeaders();
        HttpEntity<OfferEditDTO> request = new HttpEntity<>(updatedOffer, headers);
//When
        ResponseEntity<OfferDTO> actual = restTemplate.exchange(
                "/api/owner/offer",
                HttpMethod.PUT,
                request,
                OfferDTO.class);
//Then
        ConsumerRecords<String, String> consumedRecord = consumer.poll(Duration.ofSeconds(5));

        assertNotNull(consumedRecord);
        ConsumerRecord<String, String> record = consumedRecord.iterator().next();

        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertOfferFields(requireNonNull(actual.getBody()), UPDATED_NAME);
        assertKafkaPayload(actual.getBody(), record);
    }

    @Test
    public void testDeleteOffer() {
//Given
        long offerId = populateDatabase().getId();

        HttpHeaders headers = createTestHttpHeaders();
        HttpEntity<?> request = new HttpEntity<>(headers);
//When
        restTemplate.exchange(
                "/api/owner/offer/" + offerId,
                HttpMethod.DELETE,
                request,
                String.class);
//Then
        ResponseEntity<OfferDTO[]> savedOffers = restTemplate.getForEntity("/api/offers", OfferDTO[].class);

        List<OfferDTO> offers = Arrays.asList(requireNonNull(savedOffers.getBody()));

        assertTrue(offers
                .stream()
                .filter(offerDTO -> offerDTO.getId().equals(TEST_DEFAULT_OFFER_ID))
                .findFirst()
                .isEmpty());

        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, OFFER_TOPIC);

        assertTrue(record.value().contains("Offer with ID"));
        assertTrue(record.value().contains("was deleted"));
    }

    private static HttpHeaders createTestHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Forwarded-User", TEST_OWNER_EMAIL);
        return headers;
    }

    private Offer populateDatabase() {
        Offer offer = getPopulatedOffer(2L);
        offerRepository.save(offer);

        Offer offer1 = getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
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
