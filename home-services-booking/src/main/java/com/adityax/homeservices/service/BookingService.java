package com.adityax.homeservices.service;

import java.util.List;

import com.adityax.homeservices.dto.BookingRequest;
import com.adityax.homeservices.dto.BookingResponse;

public interface BookingService {

    BookingResponse createBooking(BookingRequest request);

    List<BookingResponse> getAllBookings();

    BookingResponse cancelBooking(Long id);
}