package com.lukk.sky.offer.adapters.api;

import com.google.gson.Gson;
import com.lukk.sky.offer.adapters.dto.KafkaPayloadModel;
import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.adapters.dto.OfferEditDTO;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.ports.notification.OfferNotificationService;
import com.lukk.sky.offer.domain.ports.service.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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

    @Operation(summary = "Hello World Page")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Welcome",
                    content = {@Content(mediaType = "application/json")}),
            @ApiResponse(responseCode = "402", description = "No user Info",
                    content = @Content)})
    @GetMapping(value = {"/", "/home"
    })
    public ResponseEntity<String> hello(@Value("${sky.helloWorld}") String message,
                                        @RequestHeader Map<String, String> headers) {
        printHeaders(headers);

        sendNotification("Offer Hello World page", Strings.EMPTY);
        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    @Operation(summary = "Get all offers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found offers",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = OfferDTO.class))})
    })
    @GetMapping("/offers")
    @CrossOrigin(origins = "${sky.crossOrigin.allowed}")
    public ResponseEntity<List<OfferDTO>> getAllOffers() {
        return ResponseEntity.ok(offerService.getAllOffers());
    }

    @Operation(summary = "Get owned offers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found owned offers",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = OfferDTO.class))}),
            @ApiResponse(responseCode = "402", description = "No user Info",
                    content = @Content)
    })
    @GetMapping("/owner/offers")
    @CrossOrigin(origins = "${sky.crossOrigin.allowed}")
    public ResponseEntity<List<OfferDTO>> getOwnedOffers(@RequestHeader Map<String, String> headers) {
        String ownerEmail = getUserInfoFromHeaders(headers);

        return ResponseEntity.ok(offerService.getOwnedOffers(ownerEmail));
    }

    @Operation(summary = "Create new offer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Offer created",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = OfferDTO.class))}),
            @ApiResponse(responseCode = "402", description = "No user Info",
                    content = @Content)
    })
    @PostMapping(value = "/owner/offer")
    @CrossOrigin(origins = "${sky.crossOrigin.allowed}")
    public ResponseEntity<?> addOffer(@Valid @RequestBody OfferDTO offer,
                                      @RequestHeader Map<String, String> headers) {
        Gson gson = new Gson();
        String ownerEmail = getUserInfoFromHeaders(headers);
        log.info("Adding new offer from owner:{}", ownerEmail);

        offer.setOwnerEmail(ownerEmail);
        OfferDTO addedOffer = offerService.addOffer(offer);

        sendNotification(gson.toJson(addedOffer), ownerEmail);
        return ResponseEntity.status(HttpStatusCode.valueOf(201)).body(addedOffer);
    }

    @Operation(summary = "Edit offer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Offer edited",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = OfferDTO.class))}),
            @ApiResponse(responseCode = "402", description = "No user Info",
                    content = @Content)
    })
    @PutMapping("/owner/offer")
    @CrossOrigin(origins = "${sky.crossOrigin.allowed}")
    public ResponseEntity<?> edit(@Valid @RequestBody OfferEditDTO offer,
                                  @RequestHeader Map<String, String> headers) {
        Gson gson = new Gson();
        String ownerEmail = getUserInfoFromHeaders(headers);
        log.info("Editing offer with ID: {} from owner:{}", offer.getId(), ownerEmail);

        offer.setOwnerEmail(ownerEmail);
        OfferDTO edited = offerService.editOffer(offer);

        sendNotification(gson.toJson(edited), ownerEmail);
        return ResponseEntity.ok(edited);
    }

    @Operation(summary = "Delete offer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Offer deleted",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = OfferDTO.class))}),
            @ApiResponse(responseCode = "402", description = "No user Info",
                    content = @Content)
    })
    @DeleteMapping("/owner/offer/{offerId}")
    @CrossOrigin(origins = "${sky.crossOrigin.allowed}")
    public ResponseEntity<?> deleteOffer(@RequestHeader Map<String, String> headers,
                                         @PathVariable String offerId) {
        Gson gson = new Gson();
        String ownerEmail = getUserInfoFromHeaders(headers);
        log.info("Deleting offer with ID:{}, from owner:{}", offerId, ownerEmail);

        offerService.deleteOffer(Long.parseLong(offerId), ownerEmail);

        sendNotification(String.format("Offer with ID: %s was deleted.", offerId), ownerEmail);
        return ResponseEntity.ok(gson.toJson(String.format("Offer with id: %s deleted.", offerId)));
    }

    @Operation(summary = "Search for offers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Offers found",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = List.class))}),
            @ApiResponse(responseCode = "402", description = "No user Info",
                    content = @Content)
    })
    @PostMapping("/search")
    @CrossOrigin(origins = "${sky.crossOrigin.allowed}")
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
        log.info("Publishing to Kafka");

        KafkaPayloadModel model = new KafkaPayloadModel(
                payload,
                LocalDateTime.now().format(DATE_TIME_FORMAT),
                owner
        );
        offerNotificationService.sendMessage(model);
    }
}
