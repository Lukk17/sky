package com.lukk.sky.booking.domain.ports.inbound;

import java.util.UUID;

public record BookingView(UUID id, UUID offerId, String bookedDate, String bookingUser, String ownerEmail) {
}
