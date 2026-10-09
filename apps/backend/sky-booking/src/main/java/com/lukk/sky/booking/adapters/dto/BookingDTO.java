package com.lukk.sky.booking.adapters.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lukk.sky.booking.domain.model.Booking;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Builder
@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingDTO {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID id;

    @NotNull
    private UUID offerId;

    @NotBlank
    @Schema(format = "date")
    private String bookedDate;

    @NotBlank
    @Email
    private String bookingUser;

    @Email
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String ownerEmail;

    public static BookingDTO of(Booking booking) {
        return BookingDTO.builder()
                .id(booking.getId())
                .offerId(booking.getOfferId())
                .bookedDate(booking.getBookedDate().toString())
                .bookingUser(booking.getBookingUser())
                .ownerEmail(booking.getOwnerEmail())
                .build();
    }
}
