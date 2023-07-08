package com.lukk.sky.offer.adapters.api;

import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.ports.service.OfferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class OfferInternalController {

    private final OfferService offerService;

    @GetMapping("/owners/{offerId}")
    public ResponseEntity<String> getOfferOwner(@PathVariable String offerId) {
        try {
            String owner = offerService.findOfferOwner(offerId);

            log.info("Found owner of offer with ID: {}", offerId);
            return ResponseEntity.ok(owner);
        } catch (OfferException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
