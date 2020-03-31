package com.lukk.repository;

import com.lukk.entity.Booked;
import com.lukk.entity.Offer;
import com.lukk.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {

    List<Offer> findAllByOwner(User owner);

    Offer findOfferByBooked(Booked booked);

}
