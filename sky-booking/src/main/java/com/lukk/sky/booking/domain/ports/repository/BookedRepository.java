package com.lukk.sky.booking.domain.ports.repository;

import com.lukk.sky.booking.domain.model.Booked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookedRepository extends JpaRepository<Booked, Long> {

    List<Booked> findAllByUserEmail(String userEmail);
}
