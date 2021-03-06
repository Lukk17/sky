package com.lukk.sky.offer.dto;

import com.lukk.sky.offer.entity.Booked;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
public class OfferDTO {

    private Long id;
    private String hotelName;
    private String description;
    private String comment;
    private BigDecimal price;
    private String ownerEmail;
    private Long roomCapacity;
    private String city;
    private String country;
    private List<Booked> booked;
    private String photoPath;
}
