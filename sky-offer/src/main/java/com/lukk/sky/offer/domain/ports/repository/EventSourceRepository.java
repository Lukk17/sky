package com.lukk.sky.offer.domain.ports.repository;

import com.lukk.sky.offer.domain.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventSourceRepository extends JpaRepository<Event, Long> {

    @Query("SELECT max(e.sequenceNumber) FROM Event e WHERE e.offerId = ?1")
    Optional<Integer> findLastSequenceNumberByOfferId(Long offerId);

    @Query(value = "SELECT pg_advisory_xact_lock(:offerId)", nativeQuery = true)
    void lockOfferEventStream(@Param("offerId") long offerId);

}
