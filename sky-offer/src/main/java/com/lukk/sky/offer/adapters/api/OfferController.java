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
import java.util.Optional;

import static com.lukk.sky.offer.config.Constants.DATE_TIME_FORMAT;
import static com.lukk.sky.offer.config.Constants.USER_INFO_HEADERS;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping(path = "${sky.apiPrefix}")
public class OfferController {

    private final OfferService offerService;
    private final OfferNotificationService offerNotificationService;

    @GetMapping(value = {"/", "/home"})
    public ResponseEntity<String> hello(@Value("${sky.helloWorld}") String message,
                                        @RequestHeader Map<String, String> headers) {
        printHeaders(headers);
        try {

            String ownerEmail = getUserInfoFromHeaders(headers).orElseThrow(() -> new OfferException("No offer owner"));

            sendNotification("Offer Hello World page", ownerEmail);
            return new ResponseEntity<>(message, HttpStatus.OK);

        } catch (OfferException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<OfferDTO>> getAllOffers() {
        return ResponseEntity.ok(offerService.getAllOffers());
    }

    @PostMapping(value = "/add")
    public ResponseEntity<?> addOffer(@RequestBody OfferDTO offer, @RequestHeader Map<String, String> headers) {
        Gson gson = new Gson();

        try {
            String ownerEmail = getUserInfoFromHeaders(headers).orElseThrow(() -> new OfferException("No offer owner"));

            offer.setOwnerEmail(ownerEmail);
            OfferDTO addedOffer = offerService.addOffer(offer);

            sendNotification(gson.toJson(addedOffer), ownerEmail);
            return ResponseEntity.ok(addedOffer);

        } catch (OfferException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteOffer(@RequestBody String offerID, @RequestHeader Map<String, String> headers) {

        try {
            String ownerEmail = getUserInfoFromHeaders(headers).orElseThrow(() -> new OfferException("No offer owner"));

            offerService.deleteOffer(Long.parseLong(offerID), ownerEmail);

            sendNotification(String.format("Offer with ID: %s was deleted.", offerID), ownerEmail);
            return ResponseEntity.ok("Offer deleted.");

        } catch (OfferException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/getOwned")
    public ResponseEntity<?> getOwnedOffers(@RequestHeader Map<String, String> headers) {
        try {
            String ownerEmail = getUserInfoFromHeaders(headers).orElseThrow(() -> new OfferException("No user info"));
            List<OfferDTO> offers = offerService.getOwnedOffers(ownerEmail);

            return ResponseEntity.ok(offers);

        } catch (OfferException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/search")
    public ResponseEntity<List<OfferDTO>> search(@RequestBody String searched) {
        return ResponseEntity.ok(offerService.searchOffers(searched));

    }

    @PutMapping("/edit")
    public ResponseEntity<?> edit(@RequestBody OfferDTO offer, @RequestHeader Map<String, String> headers) {
        Gson gson = new Gson();
        try {
            String ownerEmail = getUserInfoFromHeaders(headers)
                    .orElseThrow(() -> new OfferException("No offer owner"));

            offer.setOwnerEmail(ownerEmail);
            OfferDTO edited = offerService.editOffer(offer);

            sendNotification(gson.toJson(edited), ownerEmail);
            return ResponseEntity.ok(edited);

        } catch (OfferException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private static void printHeaders(Map<String, String> headers) {
        StringBuilder str = new StringBuilder("Headers:");
        headers.forEach((key, value) -> str.append("\t").append(key).append("=").append(value));
        log.info(str.toString());
    }

    private static Optional<String> getUserInfoFromHeaders(Map<String, String> headers) {
        return headers.entrySet()
                .stream()
                .filter(entry -> USER_INFO_HEADERS.contains(entry.getKey().toLowerCase()))
                .map(Map.Entry::getValue)
                .findFirst();
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
