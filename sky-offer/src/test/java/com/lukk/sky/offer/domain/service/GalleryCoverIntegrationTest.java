package com.lukk.sky.offer.domain.service;

import com.lukk.sky.offer.AbstractIntegrationTest;
import com.lukk.sky.offer.assemblers.OfferAssembler;
import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.model.OfferPhoto;
import com.lukk.sky.offer.domain.ports.inbound.OfferService;
import com.lukk.sky.offer.domain.ports.outbound.OfferPhotoRepository;
import com.lukk.sky.offer.domain.ports.outbound.OfferRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static com.lukk.sky.common.test.TestUsers.TEST_OWNER_EMAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Gallery cover against real Postgres: set main back and forth succeeds")
class GalleryCoverIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OfferService offerService;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private OfferPhotoRepository offerPhotoRepository;

    @Test
    void setGalleryCover_whenToggledBackAndForth_thenSingleMainEachTime() {
        Offer unsaved = OfferAssembler.getPopulatedOffer(UUID.randomUUID());
        unsaved.setId(null);
        Offer offer = offerRepository.saveAndFlush(unsaved);
        UUID offerId = offer.getId();
        for (int i = 0; i < 2; i++) {
            Offer ref = offerRepository.getReferenceById(offerId);
            offerPhotoRepository.saveAndFlush(OfferPhoto.builder().offer(ref).position(i)
                    .objectKey("offers/" + offerId + "/k" + i + ".png").main(i == 0).build());
        }
        List<OfferPhoto> initial = offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId);
        UUID firstId = initial.get(0).getId();
        UUID secondId = initial.get(1).getId();

        offerService.setGalleryCover(offerId, secondId, TEST_OWNER_EMAIL);
        List<OfferPhoto> afterFirst = offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId);
        assertEquals(1, afterFirst.stream().filter(OfferPhoto::isMain).count());
        assertTrue(afterFirst.stream().filter(p -> p.getId().equals(secondId)).findFirst().orElseThrow().isMain());

        offerService.setGalleryCover(offerId, firstId, TEST_OWNER_EMAIL);
        List<OfferPhoto> afterSecond = offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId);
        assertEquals(1, afterSecond.stream().filter(OfferPhoto::isMain).count());
        assertTrue(afterSecond.stream().filter(p -> p.getId().equals(firstId)).findFirst().orElseThrow().isMain());
    }
}
