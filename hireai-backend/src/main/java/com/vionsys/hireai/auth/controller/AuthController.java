package com.vionsys.hireai.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.vionsys.hireai.auth.dto.AuthResponse;
import com.vionsys.hireai.auth.dto.LoginRequest;
import com.vionsys.hireai.auth.dto.RegisterRequest;
import com.vionsys.hireai.auth.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/candidate")
    public ResponseEntity<AuthResponse> registerCandidate(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.registerCandidate(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/register/recruiter")
    public ResponseEntity<AuthResponse> registerRecruiter(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.registerRecruiter(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

}