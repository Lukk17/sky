package com.lukk.sky.offer.adapters.outbound.persistence;

import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.ports.outbound.OfferRepository;
import com.lukk.sky.offer.domain.ports.outbound.OfferSearch;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class SpecificationOfferSearch implements OfferSearch {

    private static final List<String> SEARCHABLE_ATTRIBUTES = List.of("hotelName", "city", "country");

    private final OfferRepository offerRepository;

    @Override
    public Page<Offer> searchByTerm(String term, Pageable pageable) {
        return offerRepository.findAll(matchesAnySearchableAttribute(term), pageable);
    }

    private static Specification<Offer> matchesAnySearchableAttribute(String term) {
        if (term == null) {
            return (root, query, builder) -> builder.disjunction();
        }

        String pattern = "%" + term.toLowerCase(Locale.ROOT) + "%";

        return (root, query, builder) -> builder.or(SEARCHABLE_ATTRIBUTES.stream()
                .map(attribute -> builder.like(builder.lower(root.get(attribute)), pattern))
                .toArray(Predicate[]::new));
    }
}
