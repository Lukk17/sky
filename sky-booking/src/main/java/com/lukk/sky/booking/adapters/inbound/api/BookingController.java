package com.lukk.sky.booking.adapters.inbound.api;

import com.google.gson.Gson;
import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.adapters.dto.BookingPayload;
import com.lukk.sky.booking.domain.ports.notification.BookingNotificationService;
import com.lukk.sky.booking.domain.ports.service.BookingService;
import com.lukk.sky.common.kafka.KafkaPayloadModel;
import com.lukk.sky.common.security.IsUser;
import com.lukk.sky.common.security.SecurityUtils;
import com.lukk.sky.common.openapi.ApiCommonErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

import static com.lukk.sky.common.web.DateTimeConstants.DATE_TIME_FORMAT;

@Tag(name = "Bookings", description = "Booking lifecycle — create, list, and cancel bookings placed against offers.")
@ApiCommonErrorResponses
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping(path = "${sky.apiPrefix}", version = "1")
public class BookingController {

    private static final Gson GSON = new Gson();

    private final BookingService bookingService;
    private final BookingNotificationService bookingNotificationService;

    @Operation(summary = "Get all user's bookings (paginated)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found user bookings",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Page.class))})
    })
    @IsUser
    @GetMapping("/user/bookings")
    public ResponseEntity<Page<BookingDTO>> getBookedOffers(
            @PageableDefault(size = 20) Pageable pageable) {
        String userEmail = SecurityUtils.currentUserEmail();
        Page<BookingDTO> bookings = bookingService.getBookedOffersForUser(userEmail, pageable);

        return ResponseEntity.ok(bookings);
    }

    @Operation(summary = "Create new booking")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Booking created",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = BookingDTO.class))})
    })
    @IsUser
    @PostMapping("/bookings")
    public ResponseEntity<BookingDTO> bookOffer(@Valid @RequestBody BookingPayload bookingPayload) {
        log.info("Starting to book offer with payload: {}", bookingPayload);

        String bookingUser = SecurityUtils.currentUserEmail();
        BookingDTO bookingDTO = bookingService.bookOffer(
                bookingPayload.offerId(), bookingPayload.dateToBook(), bookingUser);

        sendNotification(GSON.toJson(bookingDTO), bookingUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(bookingDTO);
    }

    @Operation(summary = "Delete booking")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Booking deleted",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Booking not found",
                    content = @Content)
    })
    @IsUser
    @DeleteMapping("/bookings/{bookingId}")
    public ResponseEntity<Void> removeBooking(@PathVariable UUID bookingId) {
        String userEmail = SecurityUtils.currentUserEmail();
        log.info("Removing booking with ID: {} by user: {}", bookingId, userEmail);

        String removeMessage = bookingService.removeBooking(bookingId, userEmail);
        sendNotification(removeMessage, userEmail);

        return ResponseEntity.noContent().build();
    }

    private void sendNotification(String payload, String userId) {
        KafkaPayloadModel model = new KafkaPayloadModel(
                payload,
                DATE_TIME_FORMAT.format(Instant.now()),
                userId
        );
        bookingNotificationService.sendMessage(model);
    }
}
