package com.lukk.sky.offer.domain.ports.repository;

import com.lukk.sky.offer.domain.model.Offer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {

    Page<Offer> findAllByOwnerEmail(String ownerEmail, Pageable pageable);

    @Query("SELECT o FROM Offer o WHERE LOWER(o.hotelName) LIKE LOWER(CONCAT('%', :term, '%'))" +
            " OR LOWER(o.city) LIKE LOWER(CONCAT('%', :term, '%'))" +
            " OR LOWER(o.country) LIKE LOWER(CONCAT('%', :term, '%'))" +
            " OR LOWER(o.ownerEmail) LIKE LOWER(CONCAT('%', :term, '%'))")
    List<Offer> searchByTerm(@Param("term") String term);
}
