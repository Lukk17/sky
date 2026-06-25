package com.lukk.sky.offer.adapters.api;

import com.lukk.sky.common.swagger.ApiCommonErrorResponses;
import com.lukk.sky.offer.domain.ports.service.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Offers Internal", description = "Internal service-to-service API for offer owner resolution.")
@ApiCommonErrorResponses
@RestController
@RequestMapping("/api/internal/v1")
@RequiredArgsConstructor
@Slf4j
public class OfferInternalController {

    private final OfferService offerService;

    @Operation(summary = "Get offer's owner")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found owner",
                    content = {@Content(mediaType = "application/json")})
    })
    @GetMapping("/owner/offer/{offerId}")
    public ResponseEntity<String> getOfferOwner(@PathVariable Long offerId) {
        log.info("Trying to find owner of offer with ID: {}", offerId);
        String ownerEmail = offerService.findOfferOwner(offerId);

        return ResponseEntity.ok(ownerEmail);
    }

}
