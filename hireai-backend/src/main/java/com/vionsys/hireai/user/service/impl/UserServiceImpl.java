package com.vionsys.hireai.user.service.impl;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.vionsys.hireai.admin.entity.AdminAuditLog;
import com.vionsys.hireai.admin.repository.AdminAuditLogRepository;
import com.vionsys.hireai.auth.entity.EmailVerificationOtp;
import com.vionsys.hireai.auth.repository.EmailVerificationOtpRepository;
import com.vionsys.hireai.auth.repository.RefreshTokenRepository;
import com.vionsys.hireai.candidate.entity.Candidate;
import com.vionsys.hireai.candidate.entity.Resume;
import com.vionsys.hireai.candidate.enums.ResumeStatus;
import com.vionsys.hireai.candidate.exception.FileStorageException;
import com.vionsys.hireai.candidate.repository.CandidateRepository;
import com.vionsys.hireai.candidate.repository.ResumeRepository;
import com.vionsys.hireai.candidate.storage.LocalFileStorageService;
import com.vionsys.hireai.candidate.storage.ProfilePhotoStorageService;
import com.vionsys.hireai.chat.entity.DirectConversation;
import com.vionsys.hireai.chat.repository.DirectConversationRepository;
import com.vionsys.hireai.common.enums.RoleType;
import com.vionsys.hireai.email.EmailService;
import com.vionsys.hireai.exception.InvalidOtpException;
import com.vionsys.hireai.exception.UserNotFoundException;
import com.vionsys.hireai.job.entity.Job;
import com.vionsys.hireai.job.enums.JobStatus;
import com.vionsys.hireai.job.repository.JobRepository;
import com.vionsys.hireai.notification.enums.NotificationType;
import com.vionsys.hireai.notification.repository.NotificationRepository;
import com.vionsys.hireai.notification.service.NotificationService;
import com.vionsys.hireai.recruiter.entity.RecruiterProfile;
import com.vionsys.hireai.recruiter.repository.RecruiterProfileRepository;
import com.vionsys.hireai.security.CustomUserDetails;
import com.vionsys.hireai.user.dto.ChangePasswordRequest;
import com.vionsys.hireai.user.dto.DeleteAccountRequest;
import com.vionsys.hireai.user.dto.SetPasswordRequest;
import com.vionsys.hireai.user.dto.UpdateUserRequest;
import com.vionsys.hireai.user.dto.UserResponse;
import com.vionsys.hireai.user.entity.User;
import com.vionsys.hireai.user.enums.AccountStatus;
import com.vionsys.hireai.user.mapper.UserMapper;
import com.vionsys.hireai.user.repository.UserRepository;
import com.vionsys.hireai.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationOtpRepository emailVerificationOtpRepository;
    private final EmailService emailService;
    private final ProfilePhotoStorageService photoStorageService;
    private final CandidateRepository candidateRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final NotificationService notificationService;
    private final ResumeRepository resumeRepository;
    private final LocalFileStorageService fileStorageService;
    private final JobRepository jobRepository;
    private final DirectConversationRepository conversationRepository;
    private final NotificationRepository notificationRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;

    @Transactional
    @Override
    public UserResponse getCurrentUser() {
        User user = getAuthenticatedUserEntity();
        if (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) {
            String resolved = resolveAvatarUrl(user.getId());
            if (resolved != null) {
                user.setAvatarUrl(resolved);
                user = userRepository.save(user);
            }
        }
        return UserMapper.toUserResponse(user);
    }

    private String resolveAvatarUrl(UUID userId) {
        Optional<Candidate> candidateOpt = candidateRepository.findByUserId(userId);
        if (candidateOpt.isPresent() && candidateOpt.get().getProfilePhotoPath() != null) {
            return "/candidate/profile/photo";
        }

        Optional<RecruiterProfile> recruiterOpt = recruiterProfileRepository.findByUserId(userId);
        if (recruiterOpt.isPresent() && recruiterOpt.get().getProfilePhotoPath() != null) {
            return "/recruiter/profile/photo";
        }

        return null;
    }

    @Transactional(readOnly = true)
    @Override
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        return UserMapper.toUserResponse(user);
    }

    @Transactional
    @Override
    public UserResponse updateCurrentUser(UpdateUserRequest request) {
        User user = getAuthenticatedUserEntity();

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());

        userRepository.save(user);

        User updatedUser = userRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return UserMapper.toUserResponse(updatedUser);
    }

    @Transactional
    @Override
    public void changePassword(ChangePasswordRequest request) {
        User user = getAuthenticatedUserEntity();

        if (!user.hasPassword()) {
            throw new IllegalArgumentException("Your account does not have a password set. Please use the Set Password feature.");
        }

        if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
            throw new IllegalArgumentException("Current password is required");
        }

        if (request.getNewPassword() == null || request.getConfirmPassword() == null ||
                !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        String newPassword = request.getNewPassword().trim();
        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters long");
        }
        if (!newPassword.matches(".*[A-Za-z].*") || !newPassword.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("New password must contain at least one letter and one number");
        }

        // Verify current password against database hash
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        // Prevent reusing the same password
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password cannot be the same as your current password");
        }

        // Hash new password and reset lockout/failed attempts
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordSet(true);
        user.setFailedLoginAttempts(0);
        user.setLockoutUntil(null);
        userRepository.save(user);
        log.info("Password successfully changed for user: {}", user.getEmail());

        // Invalidate/revoke all active refresh tokens for this user
        refreshTokenRepository.revokeAllUserTokens(user);

        // Dispatch security notification to the user
        final User finalUser = user;
        executeAfterCommit(() -> {
            try {
                emailService.sendPasswordChangedEmail(finalUser);
            } catch (Exception ex) {
                log.warn("Failed to dispatch password changed email: {}", ex.getMessage());
            }

            try {
                boolean isRecruiter = finalUser.getRole() != null && finalUser.getRole().getName() != null &&
                        finalUser.getRole().getName().name().contains("RECRUITER");
                notificationService.createAndSendNotification(
                        finalUser,
                        "Security Alert: Password Changed",
                        "Your HireAI account password was recently updated. If you did not make this change, please contact support immediately.",
                        NotificationType.SYSTEM,
                        isRecruiter ? "/recruiter/settings" : "/candidate/settings"
                );
            } catch (Exception ex) {
                log.warn("Failed to dispatch password change notification: {}", ex.getMessage());
            }
        });
    }

    @Transactional
    @Override
    public void requestSetPasswordOtp() {
        User user = getAuthenticatedUserEntity();

        if (user.hasPassword()) {
            throw new IllegalArgumentException("Account already has a password set. Use Change Password instead.");
        }

        // Rate-limit: enforce 60-second cooldown on consecutive requests
        emailVerificationOtpRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user)
                .ifPresent(latestOtp -> {
                    if (latestOtp.getCreatedAt() != null) {
                        long secondsSinceLast = Duration.between(latestOtp.getCreatedAt(), LocalDateTime.now()).getSeconds();
                        if (secondsSinceLast < 60) {
                            long wait = 60 - secondsSinceLast;
                            throw new InvalidOtpException("Please wait " + wait + " second(s) before requesting a new code.");
                        }
                    }
                    latestOtp.setUsed(true);
                    emailVerificationOtpRepository.save(latestOtp);
                });

        // Generate 6-digit OTP
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        String otpCode = String.valueOf(code);

        EmailVerificationOtp otpEntity = EmailVerificationOtp.builder()
                .user(user)
                .otpCode(otpCode)
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .attempts(0)
                .build();
        emailVerificationOtpRepository.save(otpEntity);

        emailService.sendSetPasswordOtp(user, otpCode, 10);
        log.info("Dispatched set-password OTP to user: {}", EmailService.maskEmail(user.getEmail()));
    }

    @Transactional
    @Override
    public void confirmSetPassword(SetPasswordRequest request) {
        User user = getAuthenticatedUserEntity();

        if (user.hasPassword()) {
            throw new IllegalArgumentException("Account already has a password set. Use Change Password instead.");
        }

        if (request == null || request.getOtp() == null || request.getOtp().isBlank()) {
            throw new InvalidOtpException("Verification code is required.");
        }

        if (request.getNewPassword() == null || request.getConfirmPassword() == null ||
                !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        String newPassword = request.getNewPassword().trim();
        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters long");
        }
        if (!newPassword.matches(".*[A-Za-z].*") || !newPassword.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("New password must contain at least one letter and one number");
        }

        EmailVerificationOtp otpEntity = emailVerificationOtpRepository
                .findTopByUserAndUsedFalseOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new InvalidOtpException("No pending verification code found. Please request a new code."));

        if (otpEntity.isExpired()) {
            throw new InvalidOtpException("Verification code has expired. Please request a new code.");
        }

        if (otpEntity.getAttempts() >= 5) {
            otpEntity.setUsed(true);
            emailVerificationOtpRepository.save(otpEntity);
            throw new InvalidOtpException("Too many incorrect attempts. Please request a new verification code.");
        }

        byte[] expectedBytes = otpEntity.getOtpCode().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] actualBytes = request.getOtp().trim().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        boolean codeMatches = java.security.MessageDigest.isEqual(expectedBytes, actualBytes);

        if (!codeMatches) {
            otpEntity.setAttempts(otpEntity.getAttempts() + 1);
            int remaining = 5 - otpEntity.getAttempts();
            if (remaining <= 0) {
                otpEntity.setUsed(true);
                emailVerificationOtpRepository.save(otpEntity);
                throw new InvalidOtpException("Too many incorrect attempts. Please request a new verification code.");
            }
            emailVerificationOtpRepository.save(otpEntity);
            throw new InvalidOtpException("Invalid verification code. " + remaining + " attempt(s) remaining.");
        }

        // OTP is verified successfully
        otpEntity.setUsed(true);
        emailVerificationOtpRepository.save(otpEntity);

        // Configure user password and flag
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordSet(true);
        user.setFailedLoginAttempts(0);
        user.setLockoutUntil(null);
        userRepository.save(user);
        log.info("Initial password successfully configured for OAuth user: {}", user.getEmail());

        // Revoke all refresh tokens
        refreshTokenRepository.revokeAllUserTokens(user);

        // Dispatch security notification
        try {
            boolean isRecruiter = user.getRole() != null && user.getRole().getName() != null &&
                    user.getRole().getName().name().contains("RECRUITER");
            notificationService.createAndSendNotification(
                    user,
                    "Security Alert: Password Configured",
                    "A password has been successfully configured for your HireAI account. You can now log in using your email and password as well as OAuth.",
                    NotificationType.SYSTEM,
                    isRecruiter ? "/recruiter/settings" : "/candidate/settings"
            );
        } catch (Exception ex) {
            log.warn("Failed to dispatch set-password notification: {}", ex.getMessage());
        }
    }

    @Transactional
    @Override
    public UserResponse uploadAvatar(MultipartFile file) {
        User user = getAuthenticatedUserEntity();

        // Check if user is candidate or recruiter to link their entity
        Optional<Candidate> candidateOpt = candidateRepository.findByUserId(user.getId());
        Optional<RecruiterProfile> recruiterOpt = recruiterProfileRepository.findByUserId(user.getId());

        // Delete previous photo if it exists
        if (candidateOpt.isPresent() && candidateOpt.get().getProfilePhotoPath() != null) {
            photoStorageService.deletePhoto(candidateOpt.get().getProfilePhotoPath());
        } else if (recruiterOpt.isPresent() && recruiterOpt.get().getProfilePhotoPath() != null) {
            photoStorageService.deletePhoto(recruiterOpt.get().getProfilePhotoPath());
        }

        // Store new photo safely
        String savedPath = photoStorageService.storePhoto(file);
        String photoUrl;

        if (candidateOpt.isPresent()) {
            Candidate candidate = candidateOpt.get();
            photoUrl = "/candidates/" + candidate.getId() + "/profile/photo";
            candidate.setProfilePhotoPath(savedPath);
            candidate.setProfilePhotoUrl(photoUrl);
            candidateRepository.save(candidate);
        } else if (recruiterOpt.isPresent()) {
            RecruiterProfile recruiterProfile = recruiterOpt.get();
            photoUrl = "/recruiter/profile/" + user.getId() + "/photo";
            recruiterProfile.setProfilePhotoPath(savedPath);
            recruiterProfile.setProfilePhotoUrl(photoUrl);
            recruiterProfileRepository.save(recruiterProfile);
        } else {
            photoUrl = "/users/" + user.getId() + "/avatar";
        }

        user.setAvatarUrl(photoUrl);
        User updated = userRepository.save(user);
        log.info("Updated avatar for user: {}", user.getEmail());

        return UserMapper.toUserResponse(updated);
    }

    @Transactional
    @Override
    public UserResponse deleteAvatar() {
        User user = getAuthenticatedUserEntity();

        Optional<Candidate> candidateOpt = candidateRepository.findByUserId(user.getId());
        if (candidateOpt.isPresent()) {
            Candidate candidate = candidateOpt.get();
            if (candidate.getProfilePhotoPath() != null) {
                photoStorageService.deletePhoto(candidate.getProfilePhotoPath());
                candidate.setProfilePhotoPath(null);
                candidate.setProfilePhotoUrl(null);
                candidateRepository.save(candidate);
            }
        }

        Optional<RecruiterProfile> recruiterOpt = recruiterProfileRepository.findByUserId(user.getId());
        if (recruiterOpt.isPresent()) {
            RecruiterProfile recruiterProfile = recruiterOpt.get();
            if (recruiterProfile.getProfilePhotoPath() != null) {
                photoStorageService.deletePhoto(recruiterProfile.getProfilePhotoPath());
                recruiterProfile.setProfilePhotoPath(null);
                recruiterProfile.setProfilePhotoUrl(null);
                recruiterProfileRepository.save(recruiterProfile);
            }
        }

        user.setAvatarUrl(null);
        User updated = userRepository.save(user);
        log.info("Deleted avatar for user: {}", user.getEmail());

        return UserMapper.toUserResponse(updated);
    }

    @Transactional(readOnly = true)
    @Override
    public Resource getAvatarResource(UUID userId) {
        String photoPath = getAvatarPath(userId);
        if (photoPath == null || photoPath.isBlank()) {
            throw new FileStorageException("No avatar photo found for user id: " + userId);
        }
        return photoStorageService.loadPhotoAsResource(photoPath);
    }

    @Transactional(readOnly = true)
    @Override
    public String getAvatarPath(UUID userId) {
        Optional<Candidate> candidateOpt = candidateRepository.findByUserId(userId);
        if (candidateOpt.isPresent() && candidateOpt.get().getProfilePhotoPath() != null) {
            return candidateOpt.get().getProfilePhotoPath();
        }

        Optional<RecruiterProfile> recruiterOpt = recruiterProfileRepository.findByUserId(userId);
        if (recruiterOpt.isPresent() && recruiterOpt.get().getProfilePhotoPath() != null) {
            return recruiterOpt.get().getProfilePhotoPath();
        }

        return null;
    }

    private User getAuthenticatedUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new AccessDeniedException("User is not authenticated");
        }

        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userDetails.getUsername()));
    }

    @Transactional
    @Override
    public void requestDeleteAccountOtp() {
        User user = getAuthenticatedUserEntity();

        if (user.hasPassword()) {
            throw new IllegalArgumentException("Account has a password configured. Please use your current password to confirm account deletion.");
        }

        // Check 60-second cooldown
        emailVerificationOtpRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user)
                .ifPresent(latestOtp -> {
                    if (latestOtp.getCreatedAt() != null) {
                        long secondsSinceLast = Duration.between(latestOtp.getCreatedAt(), LocalDateTime.now()).getSeconds();
                        if (secondsSinceLast < 60) {
                            long wait = 60 - secondsSinceLast;
                            throw new InvalidOtpException("Please wait " + wait + " second(s) before requesting a new code.");
                        }
                    }
                    latestOtp.setUsed(true);
                    emailVerificationOtpRepository.save(latestOtp);
                });

        // Generate 6-digit OTP
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        String otpCode = String.valueOf(code);

        EmailVerificationOtp otpEntity = EmailVerificationOtp.builder()
                .user(user)
                .otpCode(otpCode)
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .attempts(0)
                .build();
        emailVerificationOtpRepository.save(otpEntity);

        emailService.sendDeleteAccountOtp(user, otpCode, 10);
        log.info("Dispatched delete-account OTP to user: {}", EmailService.maskEmail(user.getEmail()));
    }

    @Transactional
    @Override
    public void deleteAccount(DeleteAccountRequest request) {
        User user = getAuthenticatedUserEntity();

        // 1. Verify confirmation phrase
        if (request == null || request.getConfirmationText() == null ||
                !"DELETE".equals(request.getConfirmationText().trim())) {
            throw new IllegalArgumentException("Confirmation text must be 'DELETE'");
        }

        // 2. Prevent removing the last remaining administrator
        if (user.getRole() != null && user.getRole().getName() == RoleType.ROLE_ADMIN) {
            long activeAdmins = userRepository.countActiveUnlockedAdmins();
            if (activeAdmins <= 1) {
                throw new IllegalStateException("Cannot delete the last remaining administrator account. Please assign another active administrator first.");
            }
        }

        // 3. Verification based on hasPassword
        if (user.hasPassword()) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
                throw new IllegalArgumentException("Current password is required to delete your account.");
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new BadCredentialsException("Incorrect current password. Please verify your password.");
            }
        } else {
            if (request.getOtp() == null || request.getOtp().isBlank()) {
                throw new InvalidOtpException("Verification code is required to delete your account.");
            }

            EmailVerificationOtp otpEntity = emailVerificationOtpRepository
                    .findTopByUserAndUsedFalseOrderByCreatedAtDesc(user)
                    .orElseThrow(() -> new InvalidOtpException("No pending verification code found. Please request a new code."));

            if (otpEntity.isExpired()) {
                throw new InvalidOtpException("Verification code has expired. Please request a new code.");
            }

            if (otpEntity.getAttempts() >= 5) {
                otpEntity.setUsed(true);
                emailVerificationOtpRepository.save(otpEntity);
                throw new InvalidOtpException("Too many incorrect attempts. Please request a new verification code.");
            }

            byte[] expectedBytes = otpEntity.getOtpCode().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] actualBytes = request.getOtp().trim().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            boolean codeMatches = java.security.MessageDigest.isEqual(expectedBytes, actualBytes);

            if (!codeMatches) {
                otpEntity.setAttempts(otpEntity.getAttempts() + 1);
                int remaining = 5 - otpEntity.getAttempts();
                if (remaining <= 0) {
                    otpEntity.setUsed(true);
                    emailVerificationOtpRepository.save(otpEntity);
                    throw new InvalidOtpException("Too many incorrect attempts. Please request a new verification code.");
                }
                emailVerificationOtpRepository.save(otpEntity);
                throw new InvalidOtpException("Invalid verification code. " + remaining + " attempt(s) remaining.");
            }

            // OTP verified
            otpEntity.setUsed(true);
            emailVerificationOtpRepository.save(otpEntity);
        }

        // Track files to delete safely post-commit
        List<String> photosToDelete = new ArrayList<>();
        List<String> filesToDelete = new ArrayList<>();

        // 4. Candidate soft-delete & anonymization
        Optional<Candidate> candidateOpt = candidateRepository.findByUserId(user.getId());
        if (candidateOpt.isPresent()) {
            Candidate candidate = candidateOpt.get();
            if (candidate.getProfilePhotoPath() != null) {
                photosToDelete.add(candidate.getProfilePhotoPath());
            }

            // Resume soft-delete and file tracking
            Optional<Resume> resumeOpt = resumeRepository.findByCandidateUserIdAndDeletedFalse(user.getId());
            if (resumeOpt.isPresent()) {
                Resume resume = resumeOpt.get();
                if (resume.getFilePath() != null && !resume.getFilePath().isBlank()) {
                    filesToDelete.add(resume.getFilePath());
                }
                resume.setDeleted(true);
                resume.setResumeStatus(ResumeStatus.DELETED);
                resume.setFilePath("");
                resumeRepository.save(resume);
            }

            // Anonymize candidate personal data while preserving application foreign keys
            candidate.setDeleted(true);
            candidate.setFirstName("Deleted");
            candidate.setLastName("Candidate");
            candidate.setEmail("deleted_" + candidate.getId() + "@deleted.hireai.internal");
            candidate.setPhone(null);
            candidate.setProfilePhotoPath(null);
            candidate.setProfilePhotoUrl(null);
            candidate.setBio(null);
            candidate.setLinkedinUrl(null);
            candidate.setGithubUrl(null);
            candidate.setPortfolioUrl(null);
            candidate.setCurrentCompany(null);
            candidate.setCurrentDesignation(null);
            candidateRepository.save(candidate);
        }

        // 5. Recruiter profile soft-delete & job closure
        Optional<RecruiterProfile> recruiterOpt = recruiterProfileRepository.findByUserId(user.getId());
        if (recruiterOpt.isPresent()) {
            RecruiterProfile recruiterProfile = recruiterOpt.get();
            if (recruiterProfile.getProfilePhotoPath() != null) {
                photosToDelete.add(recruiterProfile.getProfilePhotoPath());
            }
            recruiterProfile.setProfilePhotoPath(null);
            recruiterProfile.setProfilePhotoUrl(null);
            recruiterProfile.setVerified(false);
            if (recruiterProfile.getCompanyEmail() != null) {
                recruiterProfile.setCompanyEmail("deleted_" + recruiterProfile.getId() + "@deleted.hireai.internal");
            }
            recruiterProfileRepository.save(recruiterProfile);

            // Close all jobs posted by this recruiter to preserve applicant records
            List<Job> jobs = jobRepository.findByRecruiterProfileId(recruiterProfile.getId());
            for (Job job : jobs) {
                if (job.getStatus() != JobStatus.CLOSED) {
                    job.setStatus(JobStatus.CLOSED);
                    jobRepository.save(job);
                }
            }
        }

        // 6. Deactivate direct chat conversations
        List<DirectConversation> conversations = conversationRepository.findAllByAnyUserId(user.getId());
        for (DirectConversation conv : conversations) {
            conv.setIsActive(false);
            conversationRepository.save(conv);
        }

        // 7. Delete user notifications
        try {
            notificationRepository.deleteAllByUserId(user.getId());
        } catch (Exception ex) {
            log.warn("Failed to delete notifications during account deletion: {}", ex.getMessage());
        }

        // 8. Revoke all active refresh tokens & sessions
        refreshTokenRepository.revokeAllUserTokens(user);

        // 9. Soft-delete, anonymize, and free email on User entity
        String originalEmail = user.getEmail();
        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        user.setAccountStatus(AccountStatus.DELETED);
        user.setEnabled(false);
        user.setAccountNonLocked(false);
        user.setPassword(null);
        user.setPasswordSet(false);
        user.setEmail("deleted_" + user.getId() + "@deleted.hireai.internal");
        user.setFirstName("Deleted");
        user.setLastName("User");
        user.setPhoneNumber(null);
        user.setAvatarUrl(null);
        user.setProviderId(null);
        user.setAuthProvider("DELETED");
        userRepository.save(user);

        // 10. Audit log entry
        try {
            AdminAuditLog auditLog = AdminAuditLog.builder()
                    .actorUserId(user.getId())
                    .actorEmail(EmailService.maskEmail(originalEmail))
                    .action("USER_ACCOUNT_DELETED")
                    .targetUserId(user.getId())
                    .oldValue("Role=" + (user.getRole() != null && user.getRole().getName() != null ? user.getRole().getName().name() : "UNKNOWN"))
                    .newValue("STATUS=DELETED")
                    .createdAt(LocalDateTime.now())
                    .build();
            adminAuditLogRepository.save(auditLog);
        } catch (Exception ex) {
            log.warn("Failed to write audit log during account deletion: {}", ex.getMessage());
        }

        log.info("Successfully deleted and anonymized user account: ID={}, OriginalEmail={}",
                user.getId(), EmailService.maskEmail(originalEmail));

        // 11. Post-commit file cleanups & notification
        executeAfterCommit(() -> {
            try {
                emailService.sendAccountDeletedEmail(originalEmail, user.getFirstName());
            } catch (Exception ex) {
                log.warn("Failed to dispatch account deleted confirmation email: {}", ex.getMessage());
            }

            for (String photoPath : photosToDelete) {
                try {
                    photoStorageService.deletePhoto(photoPath);
                } catch (Exception ex) {
                    log.warn("Failed to delete profile photo at {}: {}", photoPath, ex.getMessage());
                }
            }
            for (String filePath : filesToDelete) {
                try {
                    fileStorageService.delete(filePath);
                } catch (Exception ex) {
                    log.warn("Failed to delete resume file at {}: {}", filePath, ex.getMessage());
                }
            }
        });
    }

    private void executeAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        task.run();
                    } catch (Exception ex) {
                        log.error("Error executing post-commit deletion task: {}", ex.getMessage(), ex);
                    }
                }
            });
        } else {
            try {
                task.run();
            } catch (Exception ex) {
                log.error("Error executing deletion task: {}", ex.getMessage(), ex);
            }
        }
    }

}