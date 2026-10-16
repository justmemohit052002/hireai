package com.adityax.homeservices.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.adityax.homeservices.entity.Booking;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

}