package com.lukk.sky.offer.adapters.api;

import com.google.gson.Gson;
import com.lukk.sky.offer.adapters.dto.KafkaPayloadModel;
import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.ports.notification.OfferNotificationService;
import com.lukk.sky.offer.domain.ports.service.OfferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.lukk.sky.offer.config.Constants.DATE_TIME_FORMAT;
import static com.lukk.sky.offer.config.Constants.USER_INFO_HEADERS;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping(path = "${sky.apiPrefix}")
public class OfferApiController {

    private final OfferService offerService;
    private final OfferNotificationService offerNotificationService;

    @GetMapping(value = {"/", "/home"})
    public ResponseEntity<String> hello(@Value("${sky.helloWorld}") String message,
                                        @RequestHeader Map<String, String> headers) {
        printHeaders(headers);
        try {

            String ownerEmail = getUserInfoFromHeaders(headers);

            sendNotification("Offer Hello World page", ownerEmail);
            return new ResponseEntity<>(message, HttpStatus.OK);

        } catch (OfferException e) {
            log.error(e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/offers")
    public ResponseEntity<List<OfferDTO>> getAllOffers() {
        return ResponseEntity.ok(offerService.getAllOffers());
    }

    @GetMapping("/owner/offers")
    public ResponseEntity<?> getOwnedOffers(@RequestHeader Map<String, String> headers) {
        try {
            String ownerEmail = getUserInfoFromHeaders(headers);
            List<OfferDTO> offers = offerService.getOwnedOffers(ownerEmail);

            return ResponseEntity.ok(offers);

        } catch (OfferException e) {
            log.error(e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(value = "/owner/offers")
    public ResponseEntity<?> addOffer(@RequestBody OfferDTO offer, @RequestHeader Map<String, String> headers) {
        Gson gson = new Gson();

        try {
            String ownerEmail = getUserInfoFromHeaders(headers);
            log.info("Adding new offer from owner:{}", ownerEmail);

            offer.setOwnerEmail(ownerEmail);
            OfferDTO addedOffer = offerService.addOffer(offer);

            sendNotification(gson.toJson(addedOffer), ownerEmail);
            return ResponseEntity.ok(addedOffer);

        } catch (OfferException e) {
            log.error(e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/owner/offers")
    public ResponseEntity<?> edit(@RequestBody OfferDTO offer, @RequestHeader Map<String, String> headers) {
        Gson gson = new Gson();
        try {
            String ownerEmail = getUserInfoFromHeaders(headers);
            log.info("Editing offer with ID: {} from owner:{}", offer.getId(), ownerEmail);

            offer.setOwnerEmail(ownerEmail);
            OfferDTO edited = offerService.editOffer(offer);

            sendNotification(gson.toJson(edited), ownerEmail);
            return ResponseEntity.ok(edited);

        } catch (OfferException e) {
            log.error(e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/owner/offers/{offerId}")
    public ResponseEntity<?> deleteOffer(@RequestHeader Map<String, String> headers, @PathVariable String offerId) {
        try {
            String ownerEmail = getUserInfoFromHeaders(headers);
            log.info("Deleting offer with ID:{}, from owner:{}", offerId, ownerEmail);

            offerService.deleteOffer(Long.parseLong(offerId), ownerEmail);

            sendNotification(String.format("Offer with ID: %s was deleted.", offerId), ownerEmail);
            return ResponseEntity.ok("Offer deleted.");

        } catch (OfferException e) {
            log.error(e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/search")
    public ResponseEntity<List<OfferDTO>> search(@RequestBody String searched) {
        return ResponseEntity.ok(offerService.searchOffers(searched));

    }

    private static void printHeaders(Map<String, String> headers) {
        StringBuilder str = new StringBuilder("Headers:");
        headers.forEach((key, value) -> str.append("\t").append(key).append("=").append(value));
        log.info(str.toString());
    }

    private static String getUserInfoFromHeaders(Map<String, String> headers) {
        return headers.entrySet()
                .stream()
                .filter(entry -> USER_INFO_HEADERS.contains(entry.getKey().toLowerCase()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new OfferException("No offer owner"));
    }

    private void sendNotification(String payload, String owner) {
        KafkaPayloadModel model = new KafkaPayloadModel(
                payload,
                LocalDateTime.now().format(DATE_TIME_FORMAT),
                owner
        );
        offerNotificationService.sendMessage(model);
    }
}
