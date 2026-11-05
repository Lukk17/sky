package com.lukk.sky.offer.adapters.inbound.api;

import com.lukk.sky.common.openapi.ApiCommonErrorResponses;
import com.lukk.sky.common.openapi.ApiDependencyUnavailableResponse;
import com.lukk.sky.common.openapi.ApiSecuredErrorResponses;
import com.lukk.sky.common.security.IsUser;
import com.lukk.sky.common.security.SecurityUtils;
import com.lukk.sky.contract.api.OffersApi;
import com.lukk.sky.contract.dto.OfferDTO;
import com.lukk.sky.contract.dto.OfferPage;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.ports.inbound.OfferService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@ApiCommonErrorResponses
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping(version = "v1")
public class OfferQueryController implements OffersApi {

    private static final int SEARCH_TERM_MAX_LENGTH = 100;

    private final OfferService offerService;

    @ApiResponse(
            responseCode = "200",
            description = "Found offers",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OfferPage.class)))
    @SecurityRequirements
    @ApiDependencyUnavailableResponse
    @Override
    public ResponseEntity<OfferPage> getAllOffers(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(OfferContractMapper.toPage(offerService.getAllOffers(pageable)));
    }

    @ApiResponse(
            responseCode = "200",
            description = "Offer found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OfferDTO.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Offer not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @SecurityRequirements
    @ApiDependencyUnavailableResponse
    @Override
    public ResponseEntity<OfferDTO> getOfferById(UUID offerId) {
        return ResponseEntity.ok(OfferContractMapper.toDto(offerService.getOfferById(offerId)));
    }

    @ApiResponse(
            responseCode = "200",
            description = "Offers found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OfferPage.class)))
    @SecurityRequirements
    @ApiDependencyUnavailableResponse
    @Override
    public ResponseEntity<OfferPage> search(String term, @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        if (term == null || term.isBlank()) {
            throw new OfferException("Search term must not be blank.");
        }

        if (term.length() > SEARCH_TERM_MAX_LENGTH) {
            throw new OfferException(
                    "Search term must not exceed " + SEARCH_TERM_MAX_LENGTH + " characters.");
        }

        return ResponseEntity.ok(OfferContractMapper.toPage(offerService.searchOffers(term, pageable)));
    }

    @ApiResponse(
            responseCode = "200",
            description = "Owner email returned",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = String.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Offer not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiSecuredErrorResponses
    @IsUser
    @Override
    public ResponseEntity<String> getOfferOwner(UUID offerId) {
        log.info("Trying to find owner of offer with ID: {}", offerId);
        String ownerEmail = offerService.findOfferOwner(offerId);

        return ResponseEntity.ok(ownerEmail);
    }
}
