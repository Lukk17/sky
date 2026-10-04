package com.lukk.sky.offer.domain.ports.inbound;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OfferView(UUID id, String hotelName, String description, String comment,
                        BigDecimal price, String ownerEmail, Long roomCapacity,
                        String city, String country, String externalPhotoUrl,
                        String photoUrl, List<GalleryPhotoView> gallery, String coverPhotoUrl) {
}
