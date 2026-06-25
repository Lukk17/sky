package com.lukk.sky.offer.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(of = {"id", "hotelName", "ownerEmail", "city"})
@Table(name = "offer", indexes = @Index(name = "idx_offer_owner_email", columnList = "owner_email"))
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String hotelName;

    @Size(max = 3000)
    private String description;

    @Size(max = 1000)
    private String comment;

    @NotNull
    @Min(value = 0)
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 100)
    @NotBlank
    @Email
    private String ownerEmail;

    @NotNull
    @Min(value = 1)
    private Long roomCapacity;

    @NotBlank
    private String city;

    @NotBlank
    private String country;

    private String photoPath;

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

        Offer other = (Offer) o;

        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
