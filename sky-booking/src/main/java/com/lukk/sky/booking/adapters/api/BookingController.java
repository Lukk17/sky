package com.lukk.sky.booking.adapters.api;

import com.google.gson.Gson;
import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.adapters.dto.KafkaPayloadModel;
import com.lukk.sky.booking.domain.exception.BookingException;
import com.lukk.sky.booking.domain.ports.notification.BookingNotificationService;
import com.lukk.sky.booking.domain.ports.service.BookingService;
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

    @GetMapping(value = {"/", "/home"})
    public ResponseEntity<String> hello(@Value("${sky.helloWorld}") String message,
                                        @RequestHeader Map<String, String> headers) {
        try {
            String userEmail = getUserInfoFromHeaders(headers);

            sendNotification("Booking Hello World page", userEmail);
            return new ResponseEntity<>(message, HttpStatus.OK);

        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/user/bookings")
    public ResponseEntity<?> getBookedOffers(@RequestHeader Map<String, String> headers) {
        try {
            String userEmail = getUserInfoFromHeaders(headers);

            List<BookingDTO> bookings = bookingService.getBookedOffersForUser(userEmail);
            return ResponseEntity.ok(bookings);

        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/bookings")
    public Mono<ResponseEntity> bookOffer(@RequestBody Map<String, String> json,
                                          @RequestHeader Map<String, String> headers) {
        Gson gson = new Gson();

        String offerId = json.get("offerId");
        String dateToBook = json.get("dateToBook");

        return Mono.fromCallable(() -> getUserInfoFromHeaders(headers))
                .flatMap(bookingUser ->
                        bookingService.bookOffer(offerId, dateToBook, bookingUser)
                                .doOnSuccess(bookingDTO -> sendNotification(gson.toJson(bookingDTO), bookingUser))
                                .map(ResponseEntity::ok)
                                .cast(ResponseEntity.class)
                )
                .doOnError(throwable -> log.info(throwable.getMessage()))
                .onErrorResume(throwable ->
                        Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(throwable.getMessage()))
                );
    }

    @DeleteMapping("/bookings/{bookingId}")
    public ResponseEntity<?> removeBooking(@RequestHeader Map<String, String> headers,
                                           @PathVariable String bookingId) {
        try {
            String userEmail = getUserInfoFromHeaders(headers);
            log.info("Removing booking with ID: {} by user: {}", bookingId, userEmail);

            String removeMessage = bookingService.removeBooking(bookingId, userEmail);
            sendNotification(removeMessage, userEmail);

            return ResponseEntity.ok(removeMessage);

        } catch (BookingException e) {
            log.error(e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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
