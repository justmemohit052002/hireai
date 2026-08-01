package com.vionsys.hireai.role.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vionsys.hireai.common.enums.RoleType;
import com.vionsys.hireai.role.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleType name);

    boolean existsByName(RoleType name);
}