package com.lukk.dto;

import com.lukk.entity.Booked;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Data
public class OfferDTO {

    private Long id;
    private String name;
    private String description;
    private String comment;
    private BigDecimal price;
    private String ownerEmail;
    private Long roomCapacity;
    private String city;
    private String country;
    private List<Booked> booked;

}
