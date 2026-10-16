package com.adityax.homeservices.service;

import java.util.List;

import com.adityax.homeservices.dto.ServiceResponse;

public interface ServiceService {

    List<ServiceResponse> getAllServices(String search);

    ServiceResponse getServiceById(Long id);

}