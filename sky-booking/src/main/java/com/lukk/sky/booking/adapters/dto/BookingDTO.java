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

    private Long id;

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
                .id(booking.getId())
                .offerId(booking.getOfferId())
                .bookedDate(booking.getBookedDate().toString())
                .bookingUser(booking.getBookingUser())
                .ownerEmail(booking.getOwnerEmail())
                .build();
    }

    public Booking toDomain() {
        return Booking.builder()
                .id(this.getId())
                .offerId(this.getOfferId())
                .bookedDate(LocalDate.parse(this.getBookedDate()))
                .bookingUser(this.getBookingUser())
                .ownerEmail(this.getOwnerEmail())
                .build();
    }
}
