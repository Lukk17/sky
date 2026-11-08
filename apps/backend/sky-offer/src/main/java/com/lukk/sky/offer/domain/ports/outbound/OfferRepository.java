package com.lukk.sky.offer.domain.ports.outbound;

import com.lukk.sky.offer.domain.model.Offer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OfferRepository extends JpaRepository<Offer, UUID>, JpaSpecificationExecutor<Offer> {

    Page<Offer> findAllByOwnerEmail(String ownerEmail, Pageable pageable);
}
