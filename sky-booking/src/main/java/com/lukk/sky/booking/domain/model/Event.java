package com.lukk.sky.booking.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(of = {"id", "bookingId", "sequenceNumber", "eventType"})
@Table(
    name = "booking_event",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_booking_event_booking_seq",
        columnNames = {"booking_id", "sequence_number"}
    )
)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long bookingId;

    private int sequenceNumber;

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String payload;

    private LocalDateTime timestamp;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }

        Event other = (Event) o;

        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
