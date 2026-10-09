package com.lukk.sky.offer.domain.ports.inbound;

import java.math.BigDecimal;

public record CreateOfferCommand(String hotelName, String description, String comment,
                                 BigDecimal price, String ownerEmail, Long roomCapacity,
                                 String city, String country, String externalPhotoUrl) {
}
