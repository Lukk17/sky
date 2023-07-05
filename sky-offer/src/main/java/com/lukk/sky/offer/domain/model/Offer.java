package com.lukk.sky.offer.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
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

    @NotBlank
    private String hotelName;

    @Size(max = 3000)
    private String description;

    private String comment;

    @NotNull
    @Min(value = 0)
    private BigDecimal price;

    private String photoPath;

    @Column(nullable = false, length = 100)
    @NotBlank
    private String ownerEmail;

    @NotNull
    @Min(value = 1)
    private Long roomCapacity;

    @NotBlank
    private String city;

    @NotBlank
    private String country;

    @OneToMany(mappedBy = "offer", cascade = CascadeType.REMOVE)
    private List<Booked> booked;

}
