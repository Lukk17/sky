package com.lukk.sky.offer.domain.ports.outbound;

import com.lukk.sky.offer.domain.model.Offer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OfferSearch {

    Page<Offer> searchByTerm(String term, Pageable pageable);
}
