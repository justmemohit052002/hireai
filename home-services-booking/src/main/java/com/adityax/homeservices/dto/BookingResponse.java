package com.adityax.homeservices.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.adityax.homeservices.entity.BookingStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Long id;
    private String serviceName;
    private String name;
    private String phone;
    private String address;
    private LocalDate date;
    private LocalTime timeSlot;
    private BookingStatus status;
}