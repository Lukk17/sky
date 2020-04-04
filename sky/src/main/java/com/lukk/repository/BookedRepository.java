package com.lukk.repository;

import com.lukk.entity.Booked;
import com.lukk.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookedRepository extends JpaRepository<Booked, Long> {

    List<Booked> findAllByUser(User user);
}