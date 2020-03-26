package com.lukk.controller;

import com.lukk.dto.OfferDTO;
import com.lukk.service.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
public class OfferController {

    private final OfferService offerService;

    @GetMapping("/getAllOffers")
    public ResponseEntity<List<OfferDTO>> getAllOffers() {
        return ResponseEntity.ok(offerService.getAllOffers());
    }

    @PostMapping(value = "/addOffer")
    public ResponseEntity<OfferDTO> addOffer(@RequestBody OfferDTO offer, Authentication auth) {
        System.out.println("\n\n\n " + offer.toString());
        offer.setOwnerEmail(
                auth.getName());
        OfferDTO addedOffer = offerService.addOffer(offer);

        return ResponseEntity.ok(addedOffer);
    }

    @DeleteMapping("/deleteOffer")
    public ResponseEntity<?> deleteOffer(@RequestBody String id, Authentication auth) {
        offerService.deleteOffer(Long.parseLong(id), auth.getName());

        return ResponseEntity.accepted().build();
    }

    @GetMapping("/getOwnedOffers")
    public ResponseEntity<List<OfferDTO>> getOwnedOffers(Authentication auth) {
        List<OfferDTO> offers = offerService.getOwnedOffers(auth.getName());

        return ResponseEntity.ok(offers);
    }

    @GetMapping("/getBookedOffers")
    public ResponseEntity<List<OfferDTO>> getBookedOffers(Authentication auth) {

        List<OfferDTO> offers = offerService.getBookedOffers(auth.getName());

        return ResponseEntity.ok(offers);
    }

    @PostMapping("/search")
    public ResponseEntity<List<OfferDTO>> search(@RequestBody String searched) {
        return ResponseEntity.ok(offerService.searchOffer(searched));

    }

    @PutMapping("/edit")
    public ResponseEntity<OfferDTO> edit(@RequestBody OfferDTO offer, Authentication auth) {
        offer.setOwnerEmail(auth.getName());
        return ResponseEntity.ok(offerService.editOffer(offer));
    }
}
