package com.adityax.homeservices.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponse {

    private Long id;

    private String name;

    private String description;

    private Double startingPrice;

    private Double rating;
}