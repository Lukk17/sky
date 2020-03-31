package com.lukk.repository;

import com.lukk.entity.Booked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookedRepository extends JpaRepository<Booked, Long> {

//    List<Booked> findAllByUser(User user);
}
