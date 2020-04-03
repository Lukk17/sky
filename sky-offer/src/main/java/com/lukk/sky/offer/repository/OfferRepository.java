package com.lukk.sky.offer.repository;

import com.lukk.sky.offer.entity.Booked;
import com.lukk.sky.offer.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {

    List<Offer> findAllByOwnerEmail(String ownerEmail);

    Offer findOfferByBooked(Booked booked);

}
