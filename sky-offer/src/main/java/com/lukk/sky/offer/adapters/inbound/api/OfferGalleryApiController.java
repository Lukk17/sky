package com.lukk.sky.offer.adapters.inbound.api;

import com.lukk.sky.common.openapi.ApiCommonErrorResponses;
import com.lukk.sky.common.openapi.ApiConflictResponse;
import com.lukk.sky.common.openapi.ApiDependencyBadGatewayResponse;
import com.lukk.sky.common.openapi.ApiDependencyUnavailableResponse;
import com.lukk.sky.common.openapi.ApiSecuredErrorResponses;
import com.lukk.sky.common.openapi.ApiUnsupportedMediaTypeResponse;
import com.lukk.sky.common.security.IsUser;
import com.lukk.sky.common.security.SecurityUtils;
import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.ports.inbound.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@ApiCommonErrorResponses
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping(path = "${sky.apiPrefix}", version = "v1")
public class OfferGalleryApiController {

    private static final String OWNER_OFFERS_TAG = "Owner offers";
    private static final String OWNER_OFFERS_TAG_DESCRIPTION =
            "Create, edit, delete, and photograph the offers the caller owns.";

    private final OfferService offerService;

    @Operation(summary = "Upload a photo to the offer gallery (owner only)",
            description = "Appends a photo at the end of the gallery, which holds at most 10 photos. "
                    + "A concurrent cover change is answered with 409.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Photo appended to gallery",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = OfferDTO.class))}),
            @ApiResponse(responseCode = "404", description = "Offer not found",
                    content = @Content),
            @ApiResponse(responseCode = "413",
                    description = "Content Too Large: gallery holds at most 10 photos.",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @Tag(name = OWNER_OFFERS_TAG, description = OWNER_OFFERS_TAG_DESCRIPTION)
    @ApiSecuredErrorResponses
    @ApiConflictResponse
    @ApiUnsupportedMediaTypeResponse
    @ApiDependencyBadGatewayResponse
    @ApiDependencyUnavailableResponse
    @IsUser
    @PostMapping(value = "/owner/offers/{offerId}/photos", consumes = "multipart/form-data")
    public ResponseEntity<OfferDTO> uploadGalleryPhoto(
            @PathVariable UUID offerId,
            @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new OfferException("Uploaded file must not be empty.");
        }
        PhotoUploadSupport.rejectWhenTooLarge(file);
        String validatedContentType = PhotoUploadSupport.detectContentType(file);
        String ownerEmail = SecurityUtils.currentUserEmail();
        InputStream inputStream = file.getInputStream();
        OfferDTO updated = OfferDtoMapper.toDto(offerService.uploadGalleryPhoto(offerId, ownerEmail, inputStream,
                file.getSize(), validatedContentType, file.getOriginalFilename()));
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Delete a gallery photo (owner only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Photo removed",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = OfferDTO.class))}),
            @ApiResponse(responseCode = "404", description = "Offer or photo not found",
                    content = @Content)
    })
    @Tag(name = OWNER_OFFERS_TAG, description = OWNER_OFFERS_TAG_DESCRIPTION)
    @ApiSecuredErrorResponses
    @IsUser
    @DeleteMapping("/owner/offers/{offerId}/photos/{photoId}")
    public ResponseEntity<OfferDTO> deleteGalleryPhoto(
            @PathVariable UUID offerId, @PathVariable UUID photoId) {
        String ownerEmail = SecurityUtils.currentUserEmail();
        return ResponseEntity.ok(OfferDtoMapper.toDto(offerService.deleteGalleryPhoto(offerId, photoId, ownerEmail)));
    }

    @Operation(summary = "Reorder a gallery photo (owner only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Gallery reordered",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = OfferDTO.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid position",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Offer or photo not found",
                    content = @Content)
    })
    @Tag(name = OWNER_OFFERS_TAG, description = OWNER_OFFERS_TAG_DESCRIPTION)
    @ApiSecuredErrorResponses
    @IsUser
    @PutMapping("/owner/offers/{offerId}/photos/{photoId}/position")
    public ResponseEntity<OfferDTO> reorderGalleryPhoto(
            @PathVariable UUID offerId, @PathVariable UUID photoId,
            @RequestParam("position") int position) {
        String ownerEmail = SecurityUtils.currentUserEmail();
        return ResponseEntity.ok(OfferDtoMapper.toDto(offerService.reorderGalleryPhoto(offerId, photoId, position, ownerEmail)));
    }

    @Operation(summary = "Set a gallery photo as cover (owner only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cover updated",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = OfferDTO.class))}),
            @ApiResponse(responseCode = "404", description = "Offer or photo not found",
                    content = @Content)
    })
    @Tag(name = OWNER_OFFERS_TAG, description = OWNER_OFFERS_TAG_DESCRIPTION)
    @ApiSecuredErrorResponses
    @IsUser
    @PutMapping("/owner/offers/{offerId}/photos/{photoId}/cover")
    public ResponseEntity<OfferDTO> setGalleryCover(
            @PathVariable UUID offerId, @PathVariable UUID photoId) {
        String ownerEmail = SecurityUtils.currentUserEmail();
        return ResponseEntity.ok(OfferDtoMapper.toDto(offerService.setGalleryCover(offerId, photoId, ownerEmail)));
    }
}
