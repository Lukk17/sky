package com.lukk.sky.offer.adapters.api;

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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.lukk.sky.offer.config.Constants.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class OfferController {

    private final OfferService offerService;
    private final OfferNotificationService offerNotificationService;

    @GetMapping(value = {"/", "/home"})
    public ResponseEntity<String> hello(@Value("${sky.helloWorld}") String message,
                                        @RequestHeader Map<String, String> headers) {
        printHeaders(headers);

        String notification = String.format("Hello World page accessed at: %s by: %s",
                LocalDateTime.now().format(DATE_TIME_FORMAT),
                getUserInfoFromHeaders(headers));
        KafkaPayloadModel model = new KafkaPayloadModel(
                "Hello World page",
                LocalDateTime.now().format(DATE_TIME_FORMAT),
                getUserInfoFromHeaders(headers)
        );
        offerNotificationService.sendMessage(model);

        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    private static List<String> getUserInfoFromHeaders(Map<String, String> headers) {
        return headers.entrySet()
                .stream()
                .filter(entry -> USER_INFO_HEADERS.contains(entry.getKey().toLowerCase()))
                .map(Map.Entry::getValue)
                .toList();
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<OfferDTO>> getAllOffers(@RequestHeader Map<String, String> headers) {
        printHeaders(headers);

        return ResponseEntity.ok(offerService.getAllOffers());
    }

    @PostMapping(value = "/add")
    public ResponseEntity<?> addOffer(@RequestBody OfferDTO offer, @RequestHeader("x-auth-request-email") String username) {
        try {
            offer.setOwnerEmail(username);
            OfferDTO addedOffer = offerService.addOffer(offer);
            return ResponseEntity.ok(addedOffer);

        } catch (OfferException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteOffer(@RequestBody String offerID, @RequestHeader("x-auth-request-email") String username) {
        try {
            offerService.deleteOffer(Long.parseLong(offerID), username);
            return ResponseEntity.accepted().build();
        } catch (OfferException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/getOwned")
    public ResponseEntity<List<OfferDTO>> getOwnedOffers(@RequestHeader("x-auth-request-email") String username) {
        List<OfferDTO> offers = offerService.getOwnedOffers(username);

        return ResponseEntity.ok(offers);
    }


    @PostMapping("/search")
    public ResponseEntity<List<OfferDTO>> search(@RequestBody String searched) {
        return ResponseEntity.ok(offerService.searchOffers(searched));

    }

    @PutMapping("/edit")
    public ResponseEntity<OfferDTO> edit(@RequestBody OfferDTO offer, @RequestHeader("x-auth-request-email") String username) {
        offer.setOwnerEmail(username);
        return ResponseEntity.ok(offerService.editOffer(offer));
    }

    private static void printHeaders(Map<String, String> headers) {
        StringBuilder str = new StringBuilder("Headers:");
        headers.forEach((key, value) -> str.append("\n").append(key).append(" : ").append(value));
        log.info(str.toString());
    }
}
