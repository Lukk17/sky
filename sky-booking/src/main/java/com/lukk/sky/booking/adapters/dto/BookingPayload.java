package com.lukk.sky.booking.adapters.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record BookingPayload(@NotNull UUID offerId, @NotNull LocalDate dateToBook) {
}
