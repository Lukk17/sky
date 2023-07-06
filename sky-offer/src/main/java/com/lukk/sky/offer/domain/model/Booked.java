package com.lukk.sky.offer.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Booked {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    private LocalDate bookedDate;

    @ManyToOne
    @JoinColumn(name = "offer")
    @NotNull
    @JsonIgnore
    @ToString.Exclude
    private Offer offer;

    @NotBlank
    private String userEmail;
}
