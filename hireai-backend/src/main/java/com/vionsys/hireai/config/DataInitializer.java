package com.vionsys.hireai.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.vionsys.hireai.common.enums.RoleType;
import com.vionsys.hireai.role.entity.Role;
import com.vionsys.hireai.role.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {

        createRoleIfNotExists(
                RoleType.ROLE_ADMIN,
                "System Administrator");

        createRoleIfNotExists(
                RoleType.ROLE_RECRUITER,
                "Recruiter");

        createRoleIfNotExists(
                RoleType.ROLE_CANDIDATE,
                "Candidate");
    }

    private void createRoleIfNotExists(RoleType roleType, String description) {

        if (!roleRepository.existsByName(roleType)) {

            Role role = Role.builder()
                    .name(roleType)
                    .description(description)
                    .build();

            roleRepository.save(role);
        }
    }
}