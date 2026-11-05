package com.lukk.sky.offer.adapters.inbound.api;

import com.lukk.sky.common.openapi.ApiCommonErrorResponses;
import com.lukk.sky.common.openapi.ApiConflictResponse;
import com.lukk.sky.common.openapi.ApiDependencyBadGatewayResponse;
import com.lukk.sky.common.openapi.ApiDependencyUnavailableResponse;
import com.lukk.sky.common.openapi.ApiSecuredErrorResponses;
import com.lukk.sky.common.openapi.ApiUnsupportedMediaTypeResponse;
import com.lukk.sky.common.security.IsUser;
import com.lukk.sky.common.security.SecurityUtils;
import com.lukk.sky.contract.api.OwnerOffersApi;
import com.lukk.sky.contract.dto.OfferDTO;
import com.lukk.sky.contract.dto.OfferEditDTO;
import com.lukk.sky.contract.dto.OfferPage;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.ports.inbound.OfferService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.UUID;

@ApiCommonErrorResponses
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping(version = "v1")
public class OwnerOfferController implements OwnerOffersApi {

    private final OfferService offerService;

    @ApiResponse(
            responseCode = "200",
            description = "Found owned offers",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OfferPage.class)))
    @ApiSecuredErrorResponses
    @ApiDependencyUnavailableResponse
    @IsUser
    @Override
    public ResponseEntity<OfferPage> getOwnedOffers(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        String ownerEmail = SecurityUtils.currentUserEmail();

        return ResponseEntity.ok(OfferContractMapper.toPage(offerService.getOwnedOffers(ownerEmail, pageable)));
    }

    @ApiResponse(
            responseCode = "201",
            description = "Offer created",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OfferDTO.class)))
    @ApiSecuredErrorResponses
    @ApiConflictResponse
    @ApiUnsupportedMediaTypeResponse
    @IsUser
    @Override
    public ResponseEntity<OfferDTO> addOffer(OfferDTO offer) {
        String ownerEmail = SecurityUtils.currentUserEmail();
        log.info("Adding new offer.");

        OfferDTO addedOffer = OfferContractMapper.toDto(
                offerService.addOffer(OfferContractMapper.toCreateCommand(offer, ownerEmail)));

        return ResponseEntity.status(HttpStatus.CREATED).body(addedOffer);
    }

    @ApiResponse(
            responseCode = "200",
            description = "Offer edited",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OfferDTO.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Offer not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiSecuredErrorResponses
    @ApiConflictResponse
    @ApiUnsupportedMediaTypeResponse
    @ApiDependencyUnavailableResponse
    @IsUser
    @Override
    public ResponseEntity<OfferDTO> edit(OfferEditDTO offer) {
        String ownerEmail = SecurityUtils.currentUserEmail();
        log.info("Editing offer with ID: {}", offer.getId());

        OfferDTO edited = OfferContractMapper.toDto(
                offerService.editOffer(OfferContractMapper.toEditCommand(offer), ownerEmail));

        return ResponseEntity.ok(edited);
    }

    @ApiResponse(responseCode = "204", description = "Offer deleted")
    @ApiResponse(
            responseCode = "404",
            description = "Offer not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiSecuredErrorResponses
    @ApiConflictResponse
    @IsUser
    @Override
    @RequestMapping(
            method = RequestMethod.DELETE,
            value = OwnerOffersApi.PATH_DELETE_OFFER,
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
    public ResponseEntity<Void> deleteOffer(UUID offerId) {
        String ownerEmail = SecurityUtils.currentUserEmail();
        log.info("Deleting offer with ID: {}", offerId);

        offerService.deleteOffer(offerId, ownerEmail);

        return ResponseEntity.noContent().build();
    }

    @ApiResponse(
            responseCode = "200",
            description = "Photo uploaded; updated offer returned with photoUrl",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OfferDTO.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Offer not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "413",
            description = "Content Too Large: the upload exceeds the file or gallery limit",
            content = @Content(
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiSecuredErrorResponses
    @ApiUnsupportedMediaTypeResponse
    @ApiDependencyBadGatewayResponse
    @ApiDependencyUnavailableResponse
    @IsUser
    @Override
    public ResponseEntity<OfferDTO> uploadPhoto(UUID offerId, MultipartFile file) {
        String validatedContentType = validatedContentTypeOf(file);

        String ownerEmail = SecurityUtils.currentUserEmail();
        log.info("Uploading photo for offer ID: {}", offerId);

        OfferDTO updated = OfferContractMapper.toDto(offerService.uploadPhoto(
                offerId,
                ownerEmail,
                contentOf(file),
                file.getSize(),
                validatedContentType,
                file.getOriginalFilename()
        ));

        return ResponseEntity.ok(updated);
    }

    @ApiResponse(responseCode = "204", description = "Photo removed from storage and cleared on the offer")
    @ApiResponse(
            responseCode = "404",
            description = "Offer not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiSecuredErrorResponses
    @IsUser
    @Override
    @RequestMapping(
            method = RequestMethod.DELETE,
            value = OwnerOffersApi.PATH_DELETE_PHOTO,
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
    public ResponseEntity<Void> deletePhoto(UUID offerId) {
        String ownerEmail = SecurityUtils.currentUserEmail();
        log.info("Deleting photo of offer ID: {}", offerId);

        offerService.deletePhoto(offerId, ownerEmail);

        return ResponseEntity.noContent().build();
    }

    @ApiResponse(
            responseCode = "200",
            description = "Photo appended to gallery",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OfferDTO.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Offer not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "413",
            description = "Content Too Large: the upload exceeds the file or gallery limit",
            content = @Content(
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiSecuredErrorResponses
    @ApiConflictResponse
    @ApiUnsupportedMediaTypeResponse
    @ApiDependencyBadGatewayResponse
    @ApiDependencyUnavailableResponse
    @IsUser
    @Override
    public ResponseEntity<OfferDTO> uploadGalleryPhoto(UUID offerId, MultipartFile file) {
        String validatedContentType = validatedContentTypeOf(file);

        String ownerEmail = SecurityUtils.currentUserEmail();
        log.info("Uploading gallery photo for offer ID: {}", offerId);

        OfferDTO updated = OfferContractMapper.toDto(offerService.uploadGalleryPhoto(
                offerId,
                ownerEmail,
                contentOf(file),
                file.getSize(),
                validatedContentType,
                file.getOriginalFilename()
        ));

        return ResponseEntity.ok(updated);
    }

    @ApiResponse(
            responseCode = "200",
            description = "Photo removed",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OfferDTO.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Offer or photo not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiSecuredErrorResponses
    @IsUser
    @Override
    public ResponseEntity<OfferDTO> deleteGalleryPhoto(UUID offerId, UUID photoId) {
        String ownerEmail = SecurityUtils.currentUserEmail();

        return ResponseEntity.ok(
                OfferContractMapper.toDto(offerService.deleteGalleryPhoto(offerId, photoId, ownerEmail)));
    }

    @ApiResponse(
            responseCode = "200",
            description = "Cover updated",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OfferDTO.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Offer or photo not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiSecuredErrorResponses
    @IsUser
    @Override
    public ResponseEntity<OfferDTO> setGalleryCover(UUID offerId, UUID photoId) {
        String ownerEmail = SecurityUtils.currentUserEmail();

        return ResponseEntity.ok(
                OfferContractMapper.toDto(offerService.setGalleryCover(offerId, photoId, ownerEmail)));
    }

    @ApiResponse(
            responseCode = "200",
            description = "Gallery reordered",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OfferDTO.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Offer or photo not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiSecuredErrorResponses
    @IsUser
    @Override
    public ResponseEntity<OfferDTO> reorderGalleryPhoto(UUID offerId, UUID photoId, Integer position) {
        String ownerEmail = SecurityUtils.currentUserEmail();

        return ResponseEntity.ok(
                OfferContractMapper.toDto(offerService.reorderGalleryPhoto(offerId, photoId, position, ownerEmail)));
    }

    private static String validatedContentTypeOf(MultipartFile file) {
        if (file.isEmpty()) {
            throw new OfferException("Uploaded file must not be empty.");
        }
        PhotoUploadSupport.rejectWhenTooLarge(file);

        try {
            return PhotoUploadSupport.detectContentType(file);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read the uploaded file.", ex);
        }
    }

    private static InputStream contentOf(MultipartFile file) {
        try {
            return file.getInputStream();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read the uploaded file.", ex);
        }
    }
}
