package com.lukk.sky.booking.adapters.api;

import com.google.gson.Gson;
import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.adapters.dto.BookingPayload;
import com.lukk.sky.common.kafka.KafkaPayloadModel;
import com.lukk.sky.booking.domain.ports.notification.BookingNotificationService;
import com.lukk.sky.booking.domain.ports.service.BookingService;
import com.lukk.sky.common.security.SecurityUtils;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

import static com.lukk.sky.common.web.DateTimeConstants.DATE_TIME_FORMAT;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping(path = "${sky.apiPrefix}")
public class BookingController {

    private static final Gson GSON = new Gson();

    private final BookingService bookingService;
    private final BookingNotificationService bookingNotificationService;

    @Operation(summary = "Get all user's bookings (paginated)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found user bookings",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Page.class))}),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content)
    })
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
                            schema = @Schema(implementation = BookingDTO.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid booking request",
                    content = @Content)
    })
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
            @ApiResponse(responseCode = "200", description = "Booking deleted",
                    content = {@Content(mediaType = "application/json")}),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content)
    })
    @DeleteMapping("/bookings/{bookingId}")
    public ResponseEntity<String> removeBooking(@PathVariable String bookingId) {
        String userEmail = SecurityUtils.currentUserEmail();
        log.info("Removing booking with ID: {} by user: {}", bookingId, userEmail);

        String removeMessage = bookingService.removeBooking(bookingId, userEmail);
        sendNotification(removeMessage, userEmail);

        return ResponseEntity.ok(GSON.toJson(removeMessage));
    }

    private void sendNotification(String payload, String userId) {
        KafkaPayloadModel model = new KafkaPayloadModel(
                payload,
                LocalDateTime.now().format(DATE_TIME_FORMAT),
                userId
        );
        bookingNotificationService.sendMessage(model);
    }
}
