package com.adityax.homeservices.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.adityax.homeservices.dto.ServiceResponse;
import com.adityax.homeservices.entity.ServiceEntity;
import com.adityax.homeservices.repository.ServiceRepository;
import com.adityax.homeservices.service.ServiceService;
import com.adityax.homeservices.exception.ResourceNotFoundException;

@Service
public class ServiceServiceImpl implements ServiceService {

    private final ServiceRepository serviceRepository;

    public ServiceServiceImpl(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @Override
    public List<ServiceResponse> getAllServices(String search) {

        List<ServiceEntity> services;

        if (search == null || search.isBlank()) {
            services = serviceRepository.findAll();
        } else {
            services = serviceRepository.findByNameContainingIgnoreCase(search.trim());
        }

        return services.stream()
                .map(service -> new ServiceResponse(
                        service.getId(),
                        service.getName(),
                        service.getDescription(),
                        service.getStartingPrice(),
                        service.getRating()
                ))
                .toList();
    }

    @Override
    public ServiceResponse getServiceById(Long id) {

        ServiceEntity service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service not found with id: " + id));

        return new ServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getStartingPrice(),
                service.getRating()
        );
    }
}