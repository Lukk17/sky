package com.lukk.sky.offer.controller;

import com.lukk.sky.offer.dto.OfferDTO;
import com.lukk.sky.offer.exception.OfferException;
import com.lukk.sky.offer.service.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    @GetMapping(value = {"/", "/home"})
    public ResponseEntity<String> hello(@Value("${lukk.helloWorld}") String message) {

        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<OfferDTO>> getAllOffers() {
        return ResponseEntity.ok(offerService.getAllOffers());
    }

    @PostMapping(value = "/add")
    public ResponseEntity<?> addOffer(@RequestBody OfferDTO offer, @RequestHeader("username") String username) {
        try {
            offer.setOwnerEmail(username);
            OfferDTO addedOffer = offerService.addOffer(offer);
            return ResponseEntity.ok(addedOffer);

        } catch (OfferException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteOffer(@RequestBody String offerID, @RequestHeader("username") String username) {
        try {
            offerService.deleteOffer(Long.parseLong(offerID), username);
            return ResponseEntity.accepted().build();
        } catch (OfferException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/getOwned")
    public ResponseEntity<List<OfferDTO>> getOwnedOffers(@RequestHeader("username") String username) {
        List<OfferDTO> offers = offerService.getOwnedOffers(username);

        return ResponseEntity.ok(offers);
    }

    @GetMapping("/getBooked")
    public ResponseEntity<List<OfferDTO>> getBookedOffers(@RequestHeader("username") String username) {
        List<OfferDTO> offers = offerService.getBookedOffers(username);

        return ResponseEntity.ok(offers);
    }

    @PostMapping("/search")
    public ResponseEntity<List<OfferDTO>> search(@RequestBody String searched) {
        return ResponseEntity.ok(offerService.searchOffers(searched));

    }

    @PutMapping("/edit")
    public ResponseEntity<OfferDTO> edit(@RequestBody OfferDTO offer, @RequestHeader("username") String username) {
        offer.setOwnerEmail(username);
        return ResponseEntity.ok(offerService.editOffer(offer));
    }

    @PostMapping("/book")
    public ResponseEntity<?> bookOffer(@RequestBody Map<String, String> json, @RequestHeader("username") String username) {
        String offerID = json.get("offerID");
        String dateToBook = json.get("dateToBook");

        try {
            OfferDTO offer = offerService.bookOffer(offerID, dateToBook, username);
            return ResponseEntity.ok(offer);

        } catch (OfferException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
