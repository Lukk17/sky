package com.lukk.sky.offer.adapters.outbound.persistence;

import com.lukk.sky.offer.AbstractIntegrationTest;
import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.ports.outbound.OfferRepository;
import com.lukk.sky.offer.domain.ports.outbound.OfferSearch;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SpecificationOfferSearch: the Specification search returns the same rows as the JPQL it replaced")
class SpecificationOfferSearchTest extends AbstractIntegrationTest {

    private static final String LEGACY_SEARCH_JPQL =
            "SELECT o FROM Offer o WHERE LOWER(o.hotelName) LIKE LOWER(CONCAT('%', :term, '%'))"
                    + " OR LOWER(o.city) LIKE LOWER(CONCAT('%', :term, '%'))"
                    + " OR LOWER(o.country) LIKE LOWER(CONCAT('%', :term, '%'))";

    private static final Pageable FIRST_PAGE = PageRequest.of(0, 50, Sort.by("id"));

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private OfferSearch offerSearch;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void populateOffers() {
        offerRepository.deleteAll();
        offerRepository.saveAll(List.of(
                offer("Grand Hotel", "Warsaw", "Poland", "seaside and sun"),
                offer("Beach House", "Gdansk", "Poland", "quiet"),
                offer("Mountain Lodge", "Zakopane", "POLAND", "ski in ski out"),
                offer("Testing Inn", "Krakow", "Germany", "grand view over Warsaw")
        ));
    }

    @AfterEach
    void clearOffers() {
        offerRepository.deleteAll();
    }

    @Test
    @DisplayName("searchByTerm returns exactly the rows the previous JPQL query returned, for every probe term")
    void searchByTerm_whenComparedWithTheReplacedJpqlQuery_thenReturnsIdenticalRows() {
        List<String> probeTerms = List.of(
                "grand", "GRAND", "GrAnD", "warsaw", "pol", "POLAND", "beach", "zakopane",
                "germany", "inn", "o", "%", "_", "no-such-term", "");

        for (String term : probeTerms) {
            List<UUID> expected = legacySearch(term);
            List<UUID> actual = idsOf(offerSearch.searchByTerm(term, FIRST_PAGE));

            assertEquals(expected, actual, "term=" + term);
        }
    }

    @Test
    @DisplayName("searchByTerm matches the hotel name, the city and the country regardless of case")
    void searchByTerm_whenTermMatchesSearchableAttribute_thenReturnMatchingOffersCaseInsensitively() {
        assertEquals(1, offerSearch.searchByTerm("gRaNd hotel", FIRST_PAGE).getTotalElements());
        assertEquals(1, offerSearch.searchByTerm("ZAKOPANE", FIRST_PAGE).getTotalElements());
        assertEquals(3, offerSearch.searchByTerm("poland", FIRST_PAGE).getTotalElements());
    }

    @Test
    @DisplayName("searchByTerm ignores attributes outside the searchable set, such as the description")
    void searchByTerm_whenTermOnlyMatchesDescription_thenReturnOnlySearchableAttributeMatches() {
        Page<Offer> found = offerSearch.searchByTerm("grand", FIRST_PAGE);

        assertEquals(1, found.getTotalElements());
        assertEquals("Grand Hotel", found.getContent().get(0).getHotelName());
    }

    @Test
    @DisplayName("searchByTerm returns an empty page for a null term, as the replaced JPQL query did")
    void searchByTerm_whenTermIsNull_thenReturnEmptyPage() {
        Page<Offer> found = offerSearch.searchByTerm(null, FIRST_PAGE);

        assertTrue(found.isEmpty());
        assertEquals(0, found.getTotalElements());
    }

    @Test
    @DisplayName("searchByTerm honours the requested page size and reports the full match count")
    void searchByTerm_whenMoreMatchesThanPageSize_thenReturnRequestedPageAndTotalCount() {
        Page<Offer> firstPage = offerSearch.searchByTerm("poland", PageRequest.of(0, 2, Sort.by("id")));

        assertEquals(2, firstPage.getContent().size());
        assertEquals(3, firstPage.getTotalElements());
        assertEquals(2, firstPage.getTotalPages());
    }

    private List<UUID> legacySearch(String term) {
        return entityManager.createQuery(LEGACY_SEARCH_JPQL, Offer.class)
                .setParameter("term", term)
                .getResultList()
                .stream()
                .map(Offer::getId)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private static List<UUID> idsOf(Page<Offer> page) {
        return page.getContent()
                .stream()
                .map(Offer::getId)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private static Offer offer(String hotelName, String city, String country, String description) {
        return Offer.builder()
                .hotelName(hotelName)
                .city(city)
                .country(country)
                .description(description)
                .comment("comment")
                .ownerEmail("owner@offer.com")
                .price(BigDecimal.valueOf(100))
                .roomCapacity(2L)
                .build();
    }
}
