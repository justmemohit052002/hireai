package com.vionsys.hireai.config;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vionsys.hireai.chat.entity.DirectChatMessage;
import com.vionsys.hireai.chat.entity.DirectChatParticipant;
import com.vionsys.hireai.chat.entity.DirectConversation;
import com.vionsys.hireai.chat.enums.MessageStatus;
import com.vionsys.hireai.chat.enums.MessageType;
import com.vionsys.hireai.chat.repository.DirectChatMessageRepository;
import com.vionsys.hireai.chat.repository.DirectChatParticipantRepository;
import com.vionsys.hireai.chat.repository.DirectConversationRepository;
import com.vionsys.hireai.common.enums.RoleType;
import com.vionsys.hireai.role.entity.Role;
import com.vionsys.hireai.role.repository.RoleRepository;
import com.vionsys.hireai.user.entity.User;
import com.vionsys.hireai.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final DirectConversationRepository conversationRepository;
    private final DirectChatMessageRepository messageRepository;
    private final DirectChatParticipantRepository participantRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${hireai.admin.seed.enabled:true}")
    private boolean adminSeedEnabled;

    @Value("${hireai.admin.seed.password:Password@123}")
    private String adminSeedPassword;

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Initialize Roles
        Role adminRole = createRoleIfNotExists(RoleType.ROLE_ADMIN, "System Administrator");
        Role recruiterRole = createRoleIfNotExists(RoleType.ROLE_RECRUITER, "Recruiter");
        Role candidateRole = createRoleIfNotExists(RoleType.ROLE_CANDIDATE, "Candidate");

        // 2. Initialize Seed Users if not present
        if (adminSeedEnabled && adminSeedPassword != null && !adminSeedPassword.isBlank()) {
            userRepository.findByEmail("admin@vionsys.com").orElseGet(() -> {
                User u = User.builder()
                        .firstName("System")
                        .lastName("Administrator")
                        .email("admin@vionsys.com")
                        .password(passwordEncoder.encode(adminSeedPassword))
                        .role(adminRole)
                        .enabled(true)
                        .accountNonLocked(true)
                        .build();
                log.info("[DataInitializer] Initialized system administrator account (admin@vionsys.com).");
                return userRepository.save(u);
            });
        } else {
            log.info("[DataInitializer] Admin user seeding is disabled or password unconfigured.");
        }

        User recruiter = userRepository.findByEmail("jordan.kim@vionsys.com").orElseGet(() -> {
            User u = User.builder()
                    .firstName("Jordan")
                    .lastName("Kim")
                    .email("jordan.kim@vionsys.com")
                    .password(passwordEncoder.encode("Password@123"))
                    .role(recruiterRole)
                    .enabled(true)
                    .accountNonLocked(true)
                    .build();
            return userRepository.save(u);
        });

        User candidate1 = userRepository.findByEmail("alex.rivera@example.com").orElseGet(() -> {
            User u = User.builder()
                    .firstName("Alex")
                    .lastName("Rivera")
                    .email("alex.rivera@example.com")
                    .password(passwordEncoder.encode("Password@123"))
                    .role(candidateRole)
                    .enabled(true)
                    .accountNonLocked(true)
                    .build();
            return userRepository.save(u);
        });

        User candidate2 = userRepository.findByEmail("sam.park@example.com").orElseGet(() -> {
            User u = User.builder()
                    .firstName("Sam")
                    .lastName("Park")
                    .email("sam.park@example.com")
                    .password(passwordEncoder.encode("Password@123"))
                    .role(candidateRole)
                    .enabled(true)
                    .accountNonLocked(true)
                    .build();
            return userRepository.save(u);
        });

        User candidate3 = userRepository.findByEmail("mia.torres@example.com").orElseGet(() -> {
            User u = User.builder()
                    .firstName("Mia")
                    .lastName("Torres")
                    .email("mia.torres@example.com")
                    .password(passwordEncoder.encode("Password@123"))
                    .role(candidateRole)
                    .enabled(true)
                    .accountNonLocked(true)
                    .build();
            return userRepository.save(u);
        });

        // 3. Initialize Persistent Database Conversations and Messages if empty
        if (conversationRepository.count() == 0) {
            log.info("[DataInitializer] Seeding persistent direct conversations into PostgreSQL database...");

            // Conversation 1: Jordan Kim (Recruiter) <-> Alex Rivera (Candidate)
            DirectConversation conv1 = DirectConversation.builder()
                    .recruiter(recruiter)
                    .candidate(candidate1)
                    .lastMessageText("📅 Technical & Architecture CodePair Session")
                    .lastMessageAt(LocalDateTime.now().minusMinutes(20))
                    .isActive(true)
                    .build();
            conv1 = conversationRepository.save(conv1);

            participantRepository.save(DirectChatParticipant.builder()
                    .conversation(conv1)
                    .user(recruiter)
                    .unreadCount(0)
                    .build());

            participantRepository.save(DirectChatParticipant.builder()
                    .conversation(conv1)
                    .user(candidate1)
                    .unreadCount(1)
                    .build());

            // Message 1 (Text from Recruiter)
            messageRepository.save(DirectChatMessage.builder()
                    .conversation(conv1)
                    .sender(recruiter)
                    .recipient(candidate1)
                    .messageText("Hi Alex! We reviewed your application for the Senior Frontend Engineer position and are very impressed with your background. Are you available for a 45-minute technical discussion?")
                    .messageType(MessageType.TEXT)
                    .status(MessageStatus.READ)
                    .build());

            // Message 2 (Text from Candidate)
            messageRepository.save(DirectChatMessage.builder()
                    .conversation(conv1)
                    .sender(candidate1)
                    .recipient(recruiter)
                    .messageText("Hi Jordan! Thank you so much, I would love that. I am flexible this Thursday afternoon or Friday morning PT.")
                    .messageType(MessageType.TEXT)
                    .status(MessageStatus.READ)
                    .build());

            // Message 3 (Interactive Interview Invite from Recruiter)
            String sessionMetadata = """
                    {
                        "roundTitle": "Technical & System Design Interview",
                        "date": "2026-10-15",
                        "time": "14:00",
                        "duration": 45,
                        "platform": "hireai_meet",
                        "platformLabel": "HireAI Virtual CodeRoom",
                        "meetingLink": "https://meet.hireai.dev/session/room-vionsys-8492",
                        "interviewerName": "Jordan Kim",
                        "candidateName": "Alex Rivera",
                        "jobTitle": "Senior Frontend Engineer",
                        "companyName": "HireAI / Vionsys",
                        "notes": "Please have your favorite IDE ready. We will explore React 19 concurrent features and architecture.",
                        "status": "SCHEDULED"
                    }
                    """;

            messageRepository.save(DirectChatMessage.builder()
                    .conversation(conv1)
                    .sender(recruiter)
                    .recipient(candidate1)
                    .messageText("📅 Technical & Architecture CodePair Session")
                    .messageType(MessageType.INTERVIEW_INVITE)
                    .metadataJson(sessionMetadata)
                    .status(MessageStatus.SENT)
                    .build());

            // Conversation 2: Jordan Kim (Recruiter) <-> Sam Park (Candidate)
            DirectConversation conv2 = DirectConversation.builder()
                    .recruiter(recruiter)
                    .candidate(candidate2)
                    .lastMessageText("Thanks for reaching out! I would love to learn more about the role.")
                    .lastMessageAt(LocalDateTime.now().minusHours(4))
                    .isActive(true)
                    .build();
            conv2 = conversationRepository.save(conv2);

            participantRepository.save(DirectChatParticipant.builder()
                    .conversation(conv2)
                    .user(recruiter)
                    .unreadCount(0)
                    .build());

            participantRepository.save(DirectChatParticipant.builder()
                    .conversation(conv2)
                    .user(candidate2)
                    .unreadCount(0)
                    .build());

            messageRepository.save(DirectChatMessage.builder()
                    .conversation(conv2)
                    .sender(recruiter)
                    .recipient(candidate2)
                    .messageText("Hey Sam, I saw your design portfolio on HireAI and think you would be a great fit for our Product Designer role.")
                    .messageType(MessageType.TEXT)
                    .status(MessageStatus.READ)
                    .build());

            messageRepository.save(DirectChatMessage.builder()
                    .conversation(conv2)
                    .sender(candidate2)
                    .recipient(recruiter)
                    .messageText("Thanks for reaching out! I would love to learn more about the role.")
                    .messageType(MessageType.TEXT)
                    .status(MessageStatus.READ)
                    .build());

            // Conversation 3: Jordan Kim (Recruiter) <-> Mia Torres (Candidate)
            DirectConversation conv3 = DirectConversation.builder()
                    .recruiter(recruiter)
                    .candidate(candidate3)
                    .lastMessageText("Hi Mia, congratulations on advancing to the final round!")
                    .lastMessageAt(LocalDateTime.now().minusDays(1))
                    .isActive(true)
                    .build();
            conv3 = conversationRepository.save(conv3);

            participantRepository.save(DirectChatParticipant.builder()
                    .conversation(conv3)
                    .user(recruiter)
                    .unreadCount(0)
                    .build());

            participantRepository.save(DirectChatParticipant.builder()
                    .conversation(conv3)
                    .user(candidate3)
                    .unreadCount(0)
                    .build());

            messageRepository.save(DirectChatMessage.builder()
                    .conversation(conv3)
                    .sender(recruiter)
                    .recipient(candidate3)
                    .messageText("Hi Mia, congratulations on advancing to the final round!")
                    .messageType(MessageType.TEXT)
                    .status(MessageStatus.READ)
                    .build());

            log.info("[DataInitializer] Database chat seeding completed successfully (3 conversations, 6 messages created).");
        }
    }

    private Role createRoleIfNotExists(RoleType roleType, String description) {
        return roleRepository.findByName(roleType).orElseGet(() -> {
            Role role = Role.builder()
                    .name(roleType)
                    .description(description)
                    .build();
            return roleRepository.save(role);
        });
    }
}