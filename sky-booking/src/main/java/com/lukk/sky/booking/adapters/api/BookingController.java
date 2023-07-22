package com.lukk.sky.booking.adapters.api;

import com.google.gson.Gson;
import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.adapters.dto.BookingPayload;
import com.lukk.sky.booking.adapters.dto.KafkaPayloadModel;
import com.lukk.sky.booking.domain.exception.BookingException;
import com.lukk.sky.booking.domain.ports.notification.BookingNotificationService;
import com.lukk.sky.booking.domain.ports.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.lukk.sky.booking.config.Constants.DATE_TIME_FORMAT;
import static com.lukk.sky.booking.config.Constants.USER_INFO_HEADERS;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping(path = "${sky.apiPrefix}")
public class BookingController {

    private final BookingService bookingService;
    private final BookingNotificationService bookingNotificationService;

    @Operation(summary = "Hello World Page")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Welcome",
                    content = {@Content(mediaType = "application/json")}),
            @ApiResponse(responseCode = "402", description = "No user Info",
                    content = @Content)
    })
    @GetMapping(value = {"/", "/home"})
    public ResponseEntity<String> hello(@Value("${sky.helloWorld}") String message,
                                        @RequestHeader Map<String, String> headers) {
        String userEmail = getUserInfoFromHeaders(headers);

        sendNotification("Booking Hello World page", userEmail);
        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    @Operation(summary = "Get all user's booking")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found user bookings",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = BookingDTO.class))}),
            @ApiResponse(responseCode = "402", description = "No user Info",
                    content = @Content)
    })
    @GetMapping("/user/bookings")
    @CrossOrigin(origins = "${sky.crossOrigin.allowed}")
    public ResponseEntity<?> getBookedOffers(@RequestHeader Map<String, String> headers) {
        String userEmail = getUserInfoFromHeaders(headers);

        List<BookingDTO> bookings = bookingService.getBookedOffersForUser(userEmail);
        return ResponseEntity.ok(bookings);
    }

    @Operation(summary = "Create new booking")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking created",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = BookingDTO.class))}),
            @ApiResponse(responseCode = "402", description = "No user Info",
                    content = @Content)
    })
    @PostMapping("/bookings")
    @CrossOrigin(origins = "${sky.crossOrigin.allowed}")
    public Mono<ResponseEntity> bookOffer(@Valid @RequestBody BookingPayload bookingPayload,
                                          @RequestHeader Map<String, String> headers) {
        Gson gson = new Gson();

        return Mono.fromCallable(() -> getUserInfoFromHeaders(headers))
                .flatMap(bookingUser ->
                        bookingService.bookOffer(bookingPayload.offerId(), bookingPayload.dateToBook(), bookingUser)
                                .doOnSuccess(bookingDTO -> sendNotification(gson.toJson(bookingDTO), bookingUser))
                                .map(ResponseEntity::ok)
                                .cast(ResponseEntity.class)
                )
                .doOnError(throwable -> log.info(throwable.getMessage()))
                .onErrorResume(throwable ->
                        Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(throwable.getMessage()))
                );
    }

    @Operation(summary = "Delete booking")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking deleted",
                    content = {@Content(mediaType = "application/json")}),
            @ApiResponse(responseCode = "402", description = "No user Info",
                    content = @Content)
    })
    @DeleteMapping("/bookings/{bookingId}")
    @CrossOrigin(origins = "${sky.crossOrigin.allowed}")
    public ResponseEntity<String> removeBooking(@RequestHeader Map<String, String> headers,
                                                @PathVariable String bookingId) {
        Gson gson = new Gson();
        String userEmail = getUserInfoFromHeaders(headers);
        log.info("Removing booking with ID: {} by user: {}", bookingId, userEmail);

        String removeMessage = bookingService.removeBooking(bookingId, userEmail);
        sendNotification(removeMessage, userEmail);

        return ResponseEntity.ok(gson.toJson(removeMessage));
    }

    private static String getUserInfoFromHeaders(Map<String, String> headers) {
        return headers.entrySet()
                .stream()
                .filter(entry -> USER_INFO_HEADERS.contains(entry.getKey().toLowerCase()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new BookingException("No user info found."));
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
