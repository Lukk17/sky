package com.lukk.sky.offer.adapters.api;

import com.google.gson.Gson;
import com.lukk.sky.common.kafka.KafkaPayloadModel;
import com.lukk.sky.common.security.SecurityUtils;
import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.adapters.dto.OfferEditDTO;
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
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static com.lukk.sky.common.web.DateTimeConstants.DATE_TIME_FORMAT;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping(path = "${sky.apiPrefix}")
public class OfferApiController {

    private static final Gson GSON = new Gson();

    private final OfferService offerService;
    private final OfferNotificationService offerNotificationService;

    @Operation(summary = "Get all offers (paginated)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found offers",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Page.class))})
    })
    @GetMapping("/offers")
    public ResponseEntity<Page<OfferDTO>> getAllOffers(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(offerService.getAllOffers(pageable));
    }

    @Operation(summary = "Get owned offers (paginated)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found owned offers",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Page.class))}),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content)
    })
    @GetMapping("/owner/offers")
    public ResponseEntity<Page<OfferDTO>> getOwnedOffers(
            @PageableDefault(size = 20) Pageable pageable) {
        String ownerEmail = SecurityUtils.currentUserEmail();

        return ResponseEntity.ok(offerService.getOwnedOffers(ownerEmail, pageable));
    }

    @Operation(summary = "Create new offer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Offer created",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = OfferDTO.class))}),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content)
    })
    @PostMapping("/owner/offers")
    public ResponseEntity<?> addOffer(@Valid @RequestBody OfferDTO offer) {
        String ownerEmail = SecurityUtils.currentUserEmail();
        log.info("Adding new offer from owner:{}", ownerEmail);

        offer.setOwnerEmail(ownerEmail);
        OfferDTO addedOffer = offerService.addOffer(offer);

        sendNotification(GSON.toJson(addedOffer), ownerEmail);

        return ResponseEntity.status(HttpStatusCode.valueOf(201)).body(addedOffer);
    }

    @Operation(summary = "Edit offer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Offer edited",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = OfferDTO.class))}),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content)
    })
    @PutMapping("/owner/offers")
    public ResponseEntity<?> edit(@Valid @RequestBody OfferEditDTO offer) {
        String ownerEmail = SecurityUtils.currentUserEmail();
        log.info("Editing offer with ID: {} from owner:{}", offer.getId(), ownerEmail);

        offer.setOwnerEmail(ownerEmail);
        OfferDTO edited = offerService.editOffer(offer);

        sendNotification(GSON.toJson(edited), ownerEmail);

        return ResponseEntity.ok(edited);
    }

    @Operation(summary = "Delete offer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Offer deleted",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = OfferDTO.class))}),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content)
    })
    @DeleteMapping("/owner/offers/{offerId}")
    public ResponseEntity<?> deleteOffer(@PathVariable String offerId) {
        String ownerEmail = SecurityUtils.currentUserEmail();
        log.info("Deleting offer with ID:{}, from owner:{}", offerId, ownerEmail);

        offerService.deleteOffer(Long.parseLong(offerId), ownerEmail);

        sendNotification(String.format("Offer with ID: %s was deleted.", offerId), ownerEmail);

        return ResponseEntity.ok(GSON.toJson(String.format("Offer with id: %s deleted.", offerId)));
    }

    @Operation(summary = "Search for offers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Offers found",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = List.class))})
    })
    @PostMapping("/search")
    public ResponseEntity<List<OfferDTO>> search(@RequestBody String searched) {
        return ResponseEntity.ok(offerService.searchOffers(searched));
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
