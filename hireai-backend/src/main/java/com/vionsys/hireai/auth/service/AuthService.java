package com.vionsys.hireai.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.vionsys.hireai.auth.dto.AuthResponse;
import com.vionsys.hireai.auth.dto.LoginRequest;
import com.vionsys.hireai.auth.dto.RegisterRequest;
import com.vionsys.hireai.common.enums.RoleType;
import com.vionsys.hireai.exception.RoleNotFoundException;
import com.vionsys.hireai.exception.UserAlreadyExistsException;
import com.vionsys.hireai.exception.UserNotFoundException;
import com.vionsys.hireai.role.entity.Role;
import com.vionsys.hireai.role.repository.RoleRepository;
import com.vionsys.hireai.security.CustomUserDetails;
import com.vionsys.hireai.security.jwt.JwtService;
import com.vionsys.hireai.user.entity.User;
import com.vionsys.hireai.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse registerCandidate(RegisterRequest request) {
        return register(request, RoleType.ROLE_CANDIDATE);
    }

    public AuthResponse registerRecruiter(RegisterRequest request) {
        return register(request, RoleType.ROLE_RECRUITER);
    }

    private AuthResponse register(
            RegisterRequest request,
            RoleType roleType) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        Role role = roleRepository.findByName(roleType)
                .orElseThrow(() ->
                        new RoleNotFoundException("Role not found"));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .enabled(true)
                .accountNonLocked(true)
                .role(role)
                .build();

        User savedUser = userRepository.save(user);

        CustomUserDetails userDetails =
                CustomUserDetails.fromUser(savedUser);

        String accessToken =
                jwtService.generateAccessToken(userDetails);

        String refreshToken =
                jwtService.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .userId(savedUser.getId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().getName().name())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public AuthResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        CustomUserDetails userDetails =
                CustomUserDetails.fromUser(user);

        String accessToken =
                jwtService.generateAccessToken(userDetails);

        String refreshToken =
                jwtService.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().getName().name())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

}