package com.lukk.sky.booking.domain.ports.repository;

import com.lukk.sky.booking.domain.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findAllByBookingUser(String userEmail);

    List<Booking> findAllByOfferId(String offerId);
}
