package com.lukk.sky.offer.domain.ports.inbound;

import java.math.BigDecimal;
import java.util.UUID;

public record EditOfferCommand(UUID id, String hotelName, String city, String country,
                               String description, String comment, BigDecimal price,
                               Long roomCapacity, String externalPhotoUrl) {
}
