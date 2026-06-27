package com.lukk.sky.booking.adapters.dto;

import com.lukk.sky.booking.domain.model.Booking;
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
public class BookingDTO {

    private UUID id;

    @NotNull
    private UUID offerId;

    @NotBlank
    private String bookedDate;

    @NotBlank
    @Email
    private String bookingUser;

    @Email
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
