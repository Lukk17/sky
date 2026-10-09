package com.lukk.sky.offer.domain.ports.outbound;

import com.lukk.sky.offer.domain.model.OfferPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OfferPhotoRepository extends JpaRepository<OfferPhoto, UUID> {

    List<OfferPhoto> findAllByOfferIdOrderByPositionAsc(UUID offerId);

    long countByOfferId(UUID offerId);
}
