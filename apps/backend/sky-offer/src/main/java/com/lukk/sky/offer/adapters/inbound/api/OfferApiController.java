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
import com.lukk.sky.offer.adapters.dto.OfferEditDTO;
import com.lukk.sky.offer.adapters.dto.contract.Page;
import com.lukk.sky.offer.adapters.inbound.api.contract.OffersApi;
import com.lukk.sky.offer.adapters.inbound.api.contract.OwnerOffersApi;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.ports.inbound.CreateOfferCommand;
import com.lukk.sky.offer.domain.ports.inbound.EditOfferCommand;
import com.lukk.sky.offer.domain.ports.inbound.OfferService;
import com.lukk.sky.offer.domain.ports.inbound.OfferView;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
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
public class OfferApiController implements OffersApi, OwnerOffersApi {

    private static final int SEARCH_TERM_MAX_LENGTH = 100;

    private final OfferService offerService;

    @SecurityRequirements
    @ApiDependencyUnavailableResponse
    @Override
    public ResponseEntity<Page> getAllOffers(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(OfferPageMapper.toPage(
                offerService.getAllOffers(pageable).map(OfferApiController::toDto)));
    }

    @SecurityRequirements
    @ApiDependencyUnavailableResponse
    @Override
    public ResponseEntity<OfferDTO> getOfferById(UUID offerId) {
        return ResponseEntity.ok(toDto(offerService.getOfferById(offerId)));
    }

    @SecurityRequirements
    @ApiDependencyUnavailableResponse
    @Override
    public ResponseEntity<Page> search(@PageableDefault(size = 20, sort = "id") Pageable pageable, String body) {
        if (body == null || body.isBlank()) {
            throw new OfferException("Search term must not be blank.");
        }

        if (body.length() > SEARCH_TERM_MAX_LENGTH) {
            throw new OfferException(
                    "Search term must not exceed " + SEARCH_TERM_MAX_LENGTH + " characters.");
        }

        return ResponseEntity.ok(OfferPageMapper.toPage(
                offerService.searchOffers(body, pageable).map(OfferApiController::toDto)));
    }

    @ApiSecuredErrorResponses
    @ApiDependencyUnavailableResponse
    @IsUser
    @Override
    public ResponseEntity<Page> getOwnedOffers(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        String ownerEmail = SecurityUtils.currentUserEmail();

        return ResponseEntity.ok(OfferPageMapper.toPage(
                offerService.getOwnedOffers(ownerEmail, pageable).map(OfferApiController::toDto)));
    }

    @ApiSecuredErrorResponses
    @ApiConflictResponse
    @ApiUnsupportedMediaTypeResponse
    @IsUser
    @Override
    public ResponseEntity<OfferDTO> addOffer(OfferDTO offer) {
        String ownerEmail = SecurityUtils.currentUserEmail();
        log.info("Adding new offer.");

        offer.setOwnerEmail(ownerEmail);
        OfferDTO addedOffer = toDto(offerService.addOffer(toCreateCommand(offer)));

        return ResponseEntity.status(HttpStatus.CREATED).body(addedOffer);
    }

    @ApiSecuredErrorResponses
    @ApiConflictResponse
    @ApiUnsupportedMediaTypeResponse
    @ApiDependencyUnavailableResponse
    @IsUser
    @Override
    public ResponseEntity<OfferDTO> edit(OfferEditDTO offer) {
        String ownerEmail = SecurityUtils.currentUserEmail();
        log.info("Editing offer with ID: {}", offer.getId());

        OfferDTO edited = toDto(offerService.editOffer(toEditCommand(offer), ownerEmail));

        return ResponseEntity.ok(edited);
    }

    @ApiSecuredErrorResponses
    @ApiConflictResponse
    @IsUser
    @Override
    public ResponseEntity<Void> deleteOffer(UUID offerId) {
        String ownerEmail = SecurityUtils.currentUserEmail();
        log.info("Deleting offer with ID: {}", offerId);

        offerService.deleteOffer(offerId, ownerEmail);

        return ResponseEntity.noContent().build();
    }

    @ApiSecuredErrorResponses
    @IsUser
    @Override
    public ResponseEntity<String> getOfferOwner(UUID offerId) {
        log.info("Trying to find owner of offer with ID: {}", offerId);
        String ownerEmail = offerService.findOfferOwner(offerId);

        return ResponseEntity.ok(ownerEmail);
    }

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

        OfferDTO updated = toDto(offerService.uploadPhoto(
                offerId,
                ownerEmail,
                contentOf(file),
                file.getSize(),
                validatedContentType,
                file.getOriginalFilename()
        ));

        return ResponseEntity.ok(updated);
    }

    @ApiSecuredErrorResponses
    @IsUser
    @Override
    public ResponseEntity<Void> deletePhoto(UUID offerId) {
        String ownerEmail = SecurityUtils.currentUserEmail();
        log.info("Deleting photo of offer ID: {}", offerId);

        offerService.deletePhoto(offerId, ownerEmail);

        return ResponseEntity.noContent().build();
    }

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

        OfferDTO updated = toDto(offerService.uploadGalleryPhoto(
                offerId,
                ownerEmail,
                contentOf(file),
                file.getSize(),
                validatedContentType,
                file.getOriginalFilename()
        ));

        return ResponseEntity.ok(updated);
    }

    @ApiSecuredErrorResponses
    @IsUser
    @Override
    public ResponseEntity<OfferDTO> deleteGalleryPhoto(UUID offerId, UUID photoId) {
        String ownerEmail = SecurityUtils.currentUserEmail();

        return ResponseEntity.ok(toDto(offerService.deleteGalleryPhoto(offerId, photoId, ownerEmail)));
    }

    @ApiSecuredErrorResponses
    @IsUser
    @Override
    public ResponseEntity<OfferDTO> reorderGalleryPhoto(UUID offerId, UUID photoId, Integer position) {
        String ownerEmail = SecurityUtils.currentUserEmail();

        return ResponseEntity.ok(toDto(offerService.reorderGalleryPhoto(offerId, photoId, position, ownerEmail)));
    }

    @ApiSecuredErrorResponses
    @IsUser
    @Override
    public ResponseEntity<OfferDTO> setGalleryCover(UUID offerId, UUID photoId) {
        String ownerEmail = SecurityUtils.currentUserEmail();

        return ResponseEntity.ok(toDto(offerService.setGalleryCover(offerId, photoId, ownerEmail)));
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

    private static CreateOfferCommand toCreateCommand(OfferDTO offer) {
        return new CreateOfferCommand(offer.getHotelName(), offer.getDescription(), offer.getComment(),
                offer.getPrice(), offer.getOwnerEmail(), offer.getRoomCapacity(),
                offer.getCity(), offer.getCountry(), offer.getExternalPhotoUrl());
    }

    private static EditOfferCommand toEditCommand(OfferEditDTO offer) {
        return new EditOfferCommand(offer.getId(), offer.getHotelName(), offer.getCity(), offer.getCountry(),
                offer.getDescription(), offer.getComment(), offer.getPrice(),
                offer.getRoomCapacity(), offer.getExternalPhotoUrl());
    }

    private static OfferDTO toDto(OfferView view) {
        return OfferDtoMapper.toDto(view);
    }
}
