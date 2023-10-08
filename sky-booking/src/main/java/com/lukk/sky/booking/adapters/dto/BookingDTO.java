package com.lukk.sky.booking.adapters.dto;

import com.lukk.sky.booking.domain.model.Booking;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder
@Data
@AllArgsConstructor
public class BookingDTO {

    @NotBlank
    private String offerId;

    @NotBlank
    private String bookedDate;

    @NotBlank
    @Email
    private String bookingUser;

    @Email
    private String ownerEmail;

    public static BookingDTO of(Booking booking) {
        return BookingDTO.builder()
                .offerId(booking.getOfferId())
                .bookedDate(booking.getBookedDate().toString())
                .bookingUser(booking.getBookingUser())
                .ownerEmail(booking.getOwnerEmail())
                .build();
    }

    public Booking toDomain() {
        return Booking.builder()
                .offerId(this.getOfferId())
                .bookedDate(LocalDate.parse(this.getBookedDate()))
                .bookingUser(this.getBookingUser())
                .ownerEmail(this.getOwnerEmail())
                .build();
    }
}
