package com.lukk.sky.booking.adapters.api;

import com.lukk.sky.booking.domain.exception.BookingException;
import com.lukk.sky.booking.domain.ports.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    @GetMapping("/getBooked")
    public ResponseEntity<List<OfferDTO>> getBookedOffers(@RequestHeader("x-auth-request-email") String username) {
        List<OfferDTO> offers = bookingService.getBookedOffers(username);

        return ResponseEntity.ok(offers);
    }

    @PostMapping("/book")
    public ResponseEntity<?> bookOffer(@RequestBody Map<String, String> json, @RequestHeader("x-auth-request-email") String username) {
        String offerID = json.get("offerID");
        String dateToBook = json.get("dateToBook");

        try {
            OfferDTO offer = bookingService.bookOffer(offerID, dateToBook, username);
            return ResponseEntity.ok(offer);

        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
