package com.lukk.sky.offer.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "offer_event")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long offerId;

    private int sequenceNumber;

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Lob
    @Column(columnDefinition="TEXT")
    private String payload;

    private LocalDateTime timestamp;
}
