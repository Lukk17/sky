package com.lukk.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
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

//    @ManyToOne
//    @JoinColumn(name = "user")
//    @NotNull
//    @ToString.Exclude
//    @JsonIgnore
//    private User user;
}
