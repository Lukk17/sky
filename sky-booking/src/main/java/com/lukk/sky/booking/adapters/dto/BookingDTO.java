package com.lukk.sky.booking.adapters.dto;

import com.lukk.sky.booking.domain.model.Booking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder
@Data
@AllArgsConstructor
public class BookingDTO {

    private Long id;
    private String offerId;
    private String bookedDate;
    private String bookingUser;
    private String owner;

    public static BookingDTO of(Booking booking){
        return BookingDTO.builder()
                .id(booking.getId())
                .offerId(booking.getOfferId())
                .bookedDate(booking.getBookedDate().toString())
                .bookingUser(booking.getBookingUser())
                .owner(booking.getOwner())
                .build();
    }

    public Booking toDomain(){
        return Booking.builder()
                .id(this.getId())
                .offerId(this.getOfferId())
                .bookedDate(LocalDate.parse(this.getBookedDate()))
                .bookingUser(this.getBookingUser())
                .owner(this.getOwner())
                .build();
    }
}
