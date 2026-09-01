package com.vionsys.hireai.user.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vionsys.hireai.user.dto.UpdateUserRequest;
import com.vionsys.hireai.user.dto.UserResponse;
import com.vionsys.hireai.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Self-Service", description = "Endpoints for authenticated user details and profile info")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get Current Authenticated User", description = "Fetch user account profile and role details for the logged-in session")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {

        UserResponse response = userService.getCurrentUser();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get User by ID", description = "Fetch public user information by UUID")
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable UUID userId) {

        UserResponse response = userService.getUserById(userId);

        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Update Current User", description = "Modify first name, last name, or phone number of the authenticated user")
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @RequestBody UpdateUserRequest request) {

        UserResponse response = userService.updateCurrentUser(request);

        return ResponseEntity.ok(response);
    }
}