package com.lukk.sky.offer;

import com.lukk.sky.offer.domain.model.Event;
import com.lukk.sky.offer.domain.model.EventType;
import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.ports.outbound.EventSourceRepository;
import com.lukk.sky.offer.domain.ports.outbound.OfferRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"offerTopic-1"})
@Import({TestSecurityConfig.class, TestcontainersConfiguration.class, TestS3Config.class})
@TestPropertySource(properties = "spring.flyway.locations=classpath:db/migration,classpath:db/demo")
@DisplayName("R__demo_seed: the local demo seed applies cleanly and its rows are coherent")
class DemoSeedMigrationTest {

    private static final UUID SOPOT_OFFER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID WARSAW_OFFER_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID MIAMI_OFFER_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");

    private static final List<UUID> SEEDED_OFFER_IDS = List.of(SOPOT_OFFER_ID, WARSAW_OFFER_ID, MIAMI_OFFER_ID);

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private EventSourceRepository eventSourceRepository;

    @Test
    @DisplayName("Every seeded offer is readable through the repository with its declared UUID")
    void demoSeed_whenApplied_thenSeededOffersAreReadableByUuid() {
        List<Offer> seeded = offerRepository.findAllById(SEEDED_OFFER_IDS);

        assertEquals(3, seeded.size());
        assertEquals("Sopot Beach Hotel", offerRepository.findById(SOPOT_OFFER_ID).orElseThrow().getHotelName());
        assertEquals("Warsaw City Suites", offerRepository.findById(WARSAW_OFFER_ID).orElseThrow().getHotelName());
        assertEquals(0, BigDecimal.valueOf(950.00)
                .compareTo(offerRepository.findById(MIAMI_OFFER_ID).orElseThrow().getPrice()));
    }

    @Test
    @DisplayName("Every seeded event points at an offer the same migration inserted")
    void demoSeed_whenApplied_thenEverySeededEventReferencesASeededOffer() {
        List<Event> events = eventSourceRepository.findAll();

        assertEquals(SEEDED_OFFER_IDS.size(), events.size());
        for (UUID offerId : SEEDED_OFFER_IDS) {
            Event event = events.stream()
                    .filter(candidate -> offerId.equals(candidate.getOfferId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No seeded event for offer " + offerId));

            assertEquals(EventType.OFFER_CREATED, event.getEventType());
            assertEquals(1, event.getSequenceNumber());
            assertTrue(event.getPayload().contains(offerId.toString()));
            assertTrue(offerRepository.existsById(event.getOfferId()));
        }
    }

    @Test
    @DisplayName("Every seeded offer carries an external image address and no server-owned object key")
    void demoSeed_whenApplied_thenSeededOffersCarryAnExternalAddressAndNoObjectKey() {
        for (UUID offerId : SEEDED_OFFER_IDS) {
            Offer seeded = offerRepository.findById(offerId).orElseThrow();

            assertNull(seeded.getPhotoObjectKey());
            assertTrue(seeded.getExternalPhotoUrl().startsWith("https://"));
        }
    }
}
