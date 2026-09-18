package com.lukk.sky.offer.adapters.inbound.api;

import com.lukk.sky.common.security.IsUser;
import com.lukk.sky.common.security.SecurityUtils;
import com.lukk.sky.common.openapi.ApiCommonErrorResponses;
import com.lukk.sky.common.openapi.ApiConflictResponse;
import com.lukk.sky.common.openapi.ApiDependencyBadGatewayResponse;
import com.lukk.sky.common.openapi.ApiDependencyUnavailableResponse;
import com.lukk.sky.common.openapi.ApiSecuredErrorResponses;
import com.lukk.sky.common.openapi.ApiUnsupportedMediaTypeResponse;
import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.adapters.dto.OfferEditDTO;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.ports.inbound.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

@ApiCommonErrorResponses
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping(path = "${sky.apiPrefix}", version = "v1")
public class OfferApiController {

    private static final String OFFERS_TAG = "Offers";
    private static final String OFFERS_TAG_DESCRIPTION = "Public browsing and search over the offer inventory.";
    private static final String OWNER_OFFERS_TAG = "Owner offers";
    private static final String OWNER_OFFERS_TAG_DESCRIPTION =
            "Create, edit, delete, and photograph the offers the caller owns.";

    private static final int SEARCH_TERM_MAX_LENGTH = 100;

    private static final String IMAGE_WEBP_VALUE = "image/webp";
    private static final Set<String> SNIFFED_IMAGE_TYPES =
            Set.of(MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_GIF_VALUE);

    private static final int WEBP_HEADER_LENGTH = 12;
    private static final int WEBP_MARKER_OFFSET = 8;
    private static final byte[] WEBP_RIFF = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_MARKER = {0x57, 0x45, 0x42, 0x50};

    private final OfferService offerService;

    @Operation(summary = "Get all offers (paginated)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found offers",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Page.class))})
    })
    @Tag(name = OFFERS_TAG, description = OFFERS_TAG_DESCRIPTION)
    @SecurityRequirements
    @ApiDependencyUnavailableResponse
    @GetMapping("/offers")
    public ResponseEntity<Page<OfferDTO>> getAllOffers(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(offerService.getAllOffers(pageable));
    }

    @Operation(summary = "Get owned offers (paginated)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found owned offers",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Page.class))})
    })
    @Tag(name = OWNER_OFFERS_TAG, description = OWNER_OFFERS_TAG_DESCRIPTION)
    @ApiSecuredErrorResponses
    @ApiDependencyUnavailableResponse
    @IsUser
    @GetMapping("/owner/offers")
    public ResponseEntity<Page<OfferDTO>> getOwnedOffers(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        String ownerEmail = SecurityUtils.currentUserEmail();

        return ResponseEntity.ok(offerService.getOwnedOffers(ownerEmail, pageable));
    }

    @Operation(summary = "Create new offer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Offer created",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = OfferDTO.class))})
    })
    @Tag(name = OWNER_OFFERS_TAG, description = OWNER_OFFERS_TAG_DESCRIPTION)
    @ApiSecuredErrorResponses
    @ApiConflictResponse
    @ApiUnsupportedMediaTypeResponse
    @IsUser
    @PostMapping("/owner/offers")
    public ResponseEntity<OfferDTO> addOffer(@Valid @RequestBody OfferDTO offer) {
        String ownerEmail = SecurityUtils.currentUserEmail();
        log.info("Adding new offer from owner:{}", ownerEmail);

        offer.setOwnerEmail(ownerEmail);
        OfferDTO addedOffer = offerService.addOffer(offer);

        return ResponseEntity.status(HttpStatusCode.valueOf(201)).body(addedOffer);
    }

    @Operation(summary = "Edit offer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Offer edited",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = OfferDTO.class))}),
            @ApiResponse(responseCode = "404", description = "Offer not found",
                    content = @Content)
    })
    @Tag(name = OWNER_OFFERS_TAG, description = OWNER_OFFERS_TAG_DESCRIPTION)
    @ApiSecuredErrorResponses
    @ApiConflictResponse
    @ApiUnsupportedMediaTypeResponse
    @ApiDependencyUnavailableResponse
    @IsUser
    @PutMapping("/owner/offers")
    public ResponseEntity<OfferDTO> edit(@Valid @RequestBody OfferEditDTO offer) {
        String ownerEmail = SecurityUtils.currentUserEmail();
        log.info("Editing offer with ID: {} from owner:{}", offer.getId(), ownerEmail);

        OfferDTO edited = offerService.editOffer(offer, ownerEmail);

        return ResponseEntity.ok(edited);
    }

    @Operation(summary = "Delete offer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Offer deleted",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Offer not found",
                    content = @Content)
    })
    @Tag(name = OWNER_OFFERS_TAG, description = OWNER_OFFERS_TAG_DESCRIPTION)
    @ApiSecuredErrorResponses
    @ApiConflictResponse
    @IsUser
    @DeleteMapping("/owner/offers/{offerId}")
    public ResponseEntity<Void> deleteOffer(@PathVariable UUID offerId) {
        String ownerEmail = SecurityUtils.currentUserEmail();
        log.info("Deleting offer with ID:{}, from owner:{}", offerId, ownerEmail);

        offerService.deleteOffer(offerId, ownerEmail);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search for offers (paginated)",
            description = "Searches the offer inventory for the term sent as the request body. "
                    + "The term must not be blank, and it must not be longer than "
                    + SEARCH_TERM_MAX_LENGTH + " characters. A term that breaks either rule is answered with 400.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Offers found",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Page.class))})
    })
    @Tag(name = OFFERS_TAG, description = OFFERS_TAG_DESCRIPTION)
    @SecurityRequirements
    @ApiDependencyUnavailableResponse
    @PostMapping("/search")
    public ResponseEntity<Page<OfferDTO>> search(
            @RequestBody String searched,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {

        if (searched == null || searched.isBlank()) {
            throw new OfferException("Search term must not be blank.");
        }

        if (searched.length() > SEARCH_TERM_MAX_LENGTH) {
            throw new OfferException(
                    "Search term must not exceed " + SEARCH_TERM_MAX_LENGTH + " characters.");
        }

        return ResponseEntity.ok(offerService.searchOffers(searched, pageable));
    }

    @Operation(summary = "Get the owner email for an offer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Owner email returned",
                    content = {@Content(mediaType = "application/json")}),
            @ApiResponse(responseCode = "404", description = "Offer not found",
                    content = @Content)
    })
    @Tag(name = OFFERS_TAG, description = OFFERS_TAG_DESCRIPTION)
    @GetMapping("/offers/{offerId}/owner")
    public ResponseEntity<String> getOfferOwner(@PathVariable UUID offerId) {
        log.info("Trying to find owner of offer with ID: {}", offerId);
        String ownerEmail = offerService.findOfferOwner(offerId);

        return ResponseEntity.ok(ownerEmail);
    }

    @Operation(summary = "Upload a photo for an offer (owner only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Photo uploaded; updated offer returned with photoUrl",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = OfferDTO.class))}),
            @ApiResponse(responseCode = "404", description = "Offer not found",
                    content = @Content),
            @ApiResponse(responseCode = "413",
                    description = "Content Too Large: the file is over 5 MB, or the whole multipart request is over 6 MB.",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @Tag(name = OWNER_OFFERS_TAG, description = OWNER_OFFERS_TAG_DESCRIPTION)
    @ApiSecuredErrorResponses
    @ApiUnsupportedMediaTypeResponse
    @ApiDependencyBadGatewayResponse
    @ApiDependencyUnavailableResponse
    @IsUser
    @PostMapping(value = "/owner/offers/{offerId}/photo", consumes = "multipart/form-data")
    public ResponseEntity<OfferDTO> uploadPhoto(
            @PathVariable UUID offerId,
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            throw new OfferException("Uploaded file must not be empty.");
        }

        String validatedContentType = detectContentType(file);

        String ownerEmail = SecurityUtils.currentUserEmail();
        log.info("Uploading photo for offer ID: {} from owner: {}", offerId, ownerEmail);

        InputStream inputStream = file.getInputStream();

        OfferDTO updated = offerService.uploadPhoto(
                offerId,
                ownerEmail,
                inputStream,
                file.getSize(),
                validatedContentType,
                file.getOriginalFilename()
        );

        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Delete the stored photo of an offer (owner only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Photo removed from storage and cleared on the offer",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Offer not found",
                    content = @Content)
    })
    @Tag(name = OWNER_OFFERS_TAG, description = OWNER_OFFERS_TAG_DESCRIPTION)
    @ApiSecuredErrorResponses
    @IsUser
    @DeleteMapping("/owner/offers/{offerId}/photo")
    public ResponseEntity<Void> deletePhoto(@PathVariable UUID offerId) {
        String ownerEmail = SecurityUtils.currentUserEmail();
        log.info("Deleting photo of offer ID: {} from owner: {}", offerId, ownerEmail);

        offerService.deletePhoto(offerId, ownerEmail);

        return ResponseEntity.noContent().build();
    }

    private static String detectContentType(MultipartFile file) throws IOException {
        try (InputStream content = new BufferedInputStream(file.getInputStream())) {
            if (isWebP(content)) {
                return IMAGE_WEBP_VALUE;
            }

            String sniffedContentType = URLConnection.guessContentTypeFromStream(content);

            if (sniffedContentType != null && SNIFFED_IMAGE_TYPES.contains(sniffedContentType)) {
                return sniffedContentType;
            }
        }

        throw new OfferException(
                "Unsupported image format. Allowed types: JPEG, PNG, GIF, WebP.");
    }

    private static boolean isWebP(InputStream markSupportingContent) throws IOException {
        byte[] header = new byte[WEBP_HEADER_LENGTH];

        markSupportingContent.mark(WEBP_HEADER_LENGTH);
        int read = markSupportingContent.readNBytes(header, 0, WEBP_HEADER_LENGTH);
        markSupportingContent.reset();

        return read == WEBP_HEADER_LENGTH
                && Arrays.equals(header, 0, WEBP_RIFF.length, WEBP_RIFF, 0, WEBP_RIFF.length)
                && Arrays.equals(header, WEBP_MARKER_OFFSET, WEBP_HEADER_LENGTH,
                WEBP_MARKER, 0, WEBP_MARKER.length);
    }
}
