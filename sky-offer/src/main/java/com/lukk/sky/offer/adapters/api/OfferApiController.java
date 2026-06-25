package com.lukk.sky.offer.adapters.api;

import com.google.gson.Gson;
import com.lukk.sky.common.kafka.KafkaPayloadModel;
import com.lukk.sky.common.security.SecurityUtils;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatusCode;
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

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

import static com.lukk.sky.common.web.DateTimeConstants.DATE_TIME_FORMAT;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping(path = "${sky.apiPrefix}")
public class OfferApiController {

    private static final Gson GSON = new Gson();

    private static final int MAGIC_READ_LIMIT = 12;
    private static final int SEARCH_TERM_MAX_LENGTH = 100;

    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] GIF_MAGIC = {0x47, 0x49, 0x46, 0x38};
    private static final byte[] WEBP_RIFF = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_MARKER = {0x57, 0x45, 0x42, 0x50};

    private static final Map<String, byte[]> ALLOWED_PREFIXES = Map.of(
            "image/jpeg", JPEG_MAGIC,
            "image/png", PNG_MAGIC,
            "image/gif", GIF_MAGIC
    );

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
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
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
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
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
    public ResponseEntity<?> deleteOffer(@PathVariable Long offerId) {
        String ownerEmail = SecurityUtils.currentUserEmail();
        log.info("Deleting offer with ID:{}, from owner:{}", offerId, ownerEmail);

        offerService.deleteOffer(offerId, ownerEmail);

        sendNotification(String.format("Offer with ID: %s was deleted.", offerId), ownerEmail);

        return ResponseEntity.ok(GSON.toJson(String.format("Offer with id: %s deleted.", offerId)));
    }

    @Operation(summary = "Search for offers (paginated)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Offers found",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Page.class))}),
            @ApiResponse(responseCode = "400", description = "Search term is blank or exceeds 100 characters",
                    content = @Content)
    })
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

    @Operation(summary = "Upload a photo for an offer (owner only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Photo uploaded; updated offer returned with photoUrl",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = OfferDTO.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Empty file, unsupported image type, or caller is not the owner",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content)
    })
    @PostMapping(value = "/owner/offers/{offerId}/photo", consumes = "multipart/form-data")
    public ResponseEntity<OfferDTO> uploadPhoto(
            @PathVariable Long offerId,
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

    private static String detectContentType(MultipartFile file) throws IOException {
        byte[] header = new byte[MAGIC_READ_LIMIT];
        int read;

        try (InputStream peek = file.getInputStream()) {
            read = peek.read(header, 0, MAGIC_READ_LIMIT);
        }

        if (read < 4) {
            throw new OfferException("Uploaded file is too small to be a valid image.");
        }

        if (startsWith(header, JPEG_MAGIC)) {
            return "image/jpeg";
        }

        if (startsWith(header, PNG_MAGIC)) {
            return "image/png";
        }

        if (startsWith(header, GIF_MAGIC)) {
            return "image/gif";
        }

        if (startsWith(header, WEBP_RIFF) && read >= 12 && matchesAt(header, 8, WEBP_MARKER)) {
            return "image/webp";
        }

        throw new OfferException(
                "Unsupported image format. Allowed types: JPEG, PNG, GIF, WebP.");
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }

        return Arrays.equals(data, 0, prefix.length, prefix, 0, prefix.length);
    }

    private static boolean matchesAt(byte[] data, int offset, byte[] pattern) {
        if (data.length < offset + pattern.length) {
            return false;
        }

        return Arrays.equals(data, offset, offset + pattern.length, pattern, 0, pattern.length);
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
