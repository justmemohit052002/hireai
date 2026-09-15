package com.vionsys.hireai.user.service.impl;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vionsys.hireai.exception.UserNotFoundException;
import com.vionsys.hireai.security.CustomUserDetails;
import com.vionsys.hireai.user.dto.UpdateUserRequest;
import com.vionsys.hireai.user.dto.UserResponse;
import com.vionsys.hireai.user.entity.User;
import com.vionsys.hireai.user.mapper.UserMapper;
import com.vionsys.hireai.user.repository.UserRepository;
import com.vionsys.hireai.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Override
    public UserResponse getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        return UserMapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    @Override
    public UserResponse getUserById(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        return UserMapper.toUserResponse(user);
    }
    
    @Transactional
    @Override
    public UserResponse updateCurrentUser(UpdateUserRequest request) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());

        userRepository.save(user);

        User updatedUser = userRepository.findByEmail(user.getEmail())
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        return UserMapper.toUserResponse(updatedUser);
    }

}