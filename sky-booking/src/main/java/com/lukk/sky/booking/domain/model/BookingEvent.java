package com.lukk.sky.booking.domain.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class BookingEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long offerId;

    private int sequenceNumber;

    @Enumerated(EnumType.STRING)
    private BookingEventType eventType;

    @Lob
    private String payload;

    private LocalDateTime timestamp;
}
