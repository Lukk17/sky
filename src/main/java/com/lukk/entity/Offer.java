package com.lukk.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    private String name;

    @Size(max = 3000)
    private String description;

    private String comment;

    @NotNull
    private BigDecimal price;

    private String photoPath;

    @ManyToOne
    @NotNull
    private User owner;

    private Long roomCapacity;

    @NotNull
    private String city;

    @NotNull
    private String country;

    @OneToMany(mappedBy = "offer")
    private List<Booked> booked;

}
