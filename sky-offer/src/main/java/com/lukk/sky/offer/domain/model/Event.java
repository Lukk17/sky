package com.lukk.sky.offer.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(of = {"id", "offerId", "sequenceNumber", "eventType"})
@Table(
        name = "offer_event",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_offer_event_offer_seq",
                columnNames = {"offer_id", "sequence_number"}
        )
)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Long offerId;

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
        return getClass().hashCode();
    }
}
