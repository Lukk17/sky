package com.lukk.sky.booking.adapters.dto;

import jakarta.validation.constraints.NotBlank;

public record BookingPayload(@NotBlank String offerId, @NotBlank String dateToBook) {
}
