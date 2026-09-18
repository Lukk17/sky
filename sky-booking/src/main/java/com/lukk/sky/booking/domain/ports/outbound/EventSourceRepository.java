package com.lukk.sky.booking.domain.ports.outbound;

import com.lukk.sky.booking.domain.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EventSourceRepository extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {
}
