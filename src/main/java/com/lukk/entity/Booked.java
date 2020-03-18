package com.lukk.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
public class Booked {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private LocalDate bookedDate;

    @ManyToOne
    @JoinColumn(name = "offer")
    private Offer offer;

    @ManyToOne
    @JoinColumn(name = "user")
    private User user;
}
