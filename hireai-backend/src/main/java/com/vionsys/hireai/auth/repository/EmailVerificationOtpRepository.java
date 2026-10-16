package com.vionsys.hireai.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vionsys.hireai.auth.entity.EmailVerificationOtp;
import com.vionsys.hireai.user.entity.User;

@Repository
public interface EmailVerificationOtpRepository extends JpaRepository<EmailVerificationOtp, UUID> {

    Optional<EmailVerificationOtp> findTopByUserAndUsedFalseOrderByCreatedAtDesc(User user);

    Optional<EmailVerificationOtp> findTopByUserEmailAndUsedFalseOrderByCreatedAtDesc(String email);

    void deleteByUser(User user);
}
