package com.adityax.homeservices.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.adityax.homeservices.dto.BookingRequest;
import com.adityax.homeservices.dto.BookingResponse;
import com.adityax.homeservices.entity.Booking;
import com.adityax.homeservices.entity.BookingStatus;
import com.adityax.homeservices.entity.ServiceEntity;
import com.adityax.homeservices.exception.ResourceNotFoundException;
import com.adityax.homeservices.mapper.BookingMapper;
import com.adityax.homeservices.repository.BookingRepository;
import com.adityax.homeservices.repository.ServiceRepository;
import com.adityax.homeservices.service.BookingService;

@Service
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;
    private final BookingMapper bookingMapper;

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            ServiceRepository serviceRepository,
            BookingMapper bookingMapper) {

        this.bookingRepository = bookingRepository;
        this.serviceRepository = serviceRepository;
        this.bookingMapper = bookingMapper;
    }

    @Override
    public BookingResponse createBooking(BookingRequest request) {

        ServiceEntity service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service not found with id: " + request.getServiceId()));

        Booking booking = new Booking();
        booking.setName(request.getName());
        booking.setPhone(request.getPhone());
        booking.setAddress(request.getAddress());
        booking.setDate(request.getDate());
        booking.setTimeSlot(request.getTimeSlot());
        booking.setService(service);
        booking.setStatus(BookingStatus.PENDING);

        Booking savedBooking = bookingRepository.save(booking);

        return bookingMapper.toResponse(savedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {

        return bookingRepository.findAll()
                .stream()
                .map(bookingMapper::toResponse)
                .toList();
    }

    @Override
    public BookingResponse cancelBooking(Long id) {

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with id: " + id));

        booking.setStatus(BookingStatus.CANCELLED);

        Booking cancelledBooking = bookingRepository.save(booking);

        return bookingMapper.toResponse(cancelledBooking);
    }
}