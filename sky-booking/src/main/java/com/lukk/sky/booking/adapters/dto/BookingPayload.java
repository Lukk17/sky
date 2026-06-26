package com.lukk.sky.booking.adapters.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BookingPayload(@NotNull UUID offerId, @NotBlank String dateToBook) {
}
