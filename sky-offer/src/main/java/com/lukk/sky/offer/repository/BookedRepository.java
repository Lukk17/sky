package com.lukk.sky.offer.repository;

import com.lukk.sky.offer.entity.Booked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookedRepository extends JpaRepository<Booked, Long> {

    List<Booked> findAllByUserEmail(String userEmail);
}
