package com.adityax.homeservices.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.adityax.homeservices.dto.ServiceResponse;
import com.adityax.homeservices.service.ServiceService;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ServiceService serviceService;

    public ServiceController(ServiceService serviceService) {
        this.serviceService = serviceService;
    }

    @GetMapping
    public ResponseEntity<List<ServiceResponse>> getAllServices(
            @RequestParam(required = false) String search) {

        return ResponseEntity.ok(serviceService.getAllServices(search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceResponse> getServiceById(
            @PathVariable Long id) {

        return ResponseEntity.ok(serviceService.getServiceById(id));
    }
}