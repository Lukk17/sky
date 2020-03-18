package com.lukk.entity;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Data
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;

    @Size(max = 3000)
    private String description;

    private String comment;

    private BigDecimal price;

    @ElementCollection
    private List<String> photos;

    @ManyToOne
    private User owner;

    private Long roomCapacity;

    private String city;

    private String country;

    @OneToMany(mappedBy = "offer")
    private List<Booked> booked;


}
