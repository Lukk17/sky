package com.lukk.sky.booking.domain.model;

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
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String offerId;

    @NotNull
    private LocalDate bookedDate;

    @NotBlank
    private String bookingUser;

    @NotBlank
    private String owner;
}
