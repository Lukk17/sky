package com.lukk.repository;

import com.lukk.entity.Booked;
import com.lukk.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {

//    List<Offer> findAllByOwner(User owner);

    Offer findOfferByBooked(Booked booked);

}
