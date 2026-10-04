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

@DisplayName("Gallery reorder against real Postgres: two sequential reorders succeed")
class GalleryReorderIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OfferService offerService;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private OfferPhotoRepository offerPhotoRepository;

    @Test
    void reorderGalleryPhoto_whenRunTwice_thenNoUniqueViolation() {
        Offer unsaved = OfferAssembler.getPopulatedOffer(UUID.randomUUID());
        unsaved.setId(null);
        Offer offer = offerRepository.saveAndFlush(unsaved);
        UUID offerId = offer.getId();
        for (int i = 0; i < 3; i++) {
            Offer ref = offerRepository.getReferenceById(offerId);
            offerPhotoRepository.saveAndFlush(OfferPhoto.builder().offer(ref).position(i)
                    .objectKey("offers/" + offerId + "/k" + i + ".png").main(i == 0).build());
        }
        List<OfferPhoto> initial = offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId);
        UUID lastId = initial.get(2).getId();

        offerService.reorderGalleryPhoto(offerId, lastId, 0, TEST_OWNER_EMAIL);
        List<OfferPhoto> afterFirst = offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId);
        assertEquals(lastId, afterFirst.get(0).getId());

        UUID nowLastId = afterFirst.get(2).getId();
        offerService.reorderGalleryPhoto(offerId, nowLastId, 0, TEST_OWNER_EMAIL);
        List<OfferPhoto> afterSecond = offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId);
        assertEquals(nowLastId, afterSecond.get(0).getId());
        for (int i = 0; i < afterSecond.size(); i++) {
            assertEquals(i, afterSecond.get(i).getPosition());
        }
        org.junit.jupiter.api.Assertions.assertTrue(afterSecond.get(0).isMain());
        org.junit.jupiter.api.Assertions.assertFalse(afterSecond.get(1).isMain());
        org.junit.jupiter.api.Assertions.assertFalse(afterSecond.get(2).isMain());
    }
}
