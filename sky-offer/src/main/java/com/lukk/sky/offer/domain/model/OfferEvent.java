package com.lukk.sky.offer.domain.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class OfferEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long offerId;

    private int sequenceNumber;

    @Enumerated(EnumType.STRING)
    private OfferEventType eventType;

    @Lob
    private String payload;

    private LocalDateTime timestamp;
}
