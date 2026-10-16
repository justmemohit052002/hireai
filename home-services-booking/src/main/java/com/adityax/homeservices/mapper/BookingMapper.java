package com.adityax.homeservices.mapper;

import org.springframework.stereotype.Component;

import com.adityax.homeservices.dto.BookingResponse;
import com.adityax.homeservices.entity.Booking;

@Component
public class BookingMapper {

    public BookingResponse toResponse(Booking booking) {

        return new BookingResponse(
                booking.getId(),
                booking.getService().getName(),
                booking.getName(),
                booking.getPhone(),
                booking.getAddress(),
                booking.getDate(),
                booking.getTimeSlot(),
                booking.getStatus()
        );
    }
}