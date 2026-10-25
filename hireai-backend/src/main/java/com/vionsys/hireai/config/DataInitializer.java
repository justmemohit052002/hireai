package com.vionsys.hireai.config;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
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
    private final Environment environment;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Value("${hireai.admin.seed.enabled:false}")
    private boolean adminSeedEnabled;

    @Value("${hireai.admin.seed.password:}")
    private String adminSeedPassword;

    @Value("${hireai.dev.demo-password:}")
    private String devDemoPassword;

    @Override
    @Transactional
    public void run(String... args) {
        // 0. Ensure PostgreSQL schema matches Candidate entity and User entity
        try {
            jdbcTemplate.execute("ALTER TABLE candidates ALTER COLUMN phone DROP NOT NULL");
            jdbcTemplate.execute("ALTER TABLE candidates DROP CONSTRAINT IF EXISTS uk5ilb7049i3j8efmvh4w7adqxk");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS password_set BOOLEAN DEFAULT TRUE NOT NULL");
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN password DROP NOT NULL");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE NOT NULL");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN DEFAULT FALSE NOT NULL");
            jdbcTemplate.execute("ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_name_check");
            jdbcTemplate.execute("ALTER TABLE roles ADD CONSTRAINT roles_name_check CHECK (((name)::text = ANY ((ARRAY['ROLE_ADMIN'::character varying, 'ROLE_RECRUITER'::character varying, 'ROLE_CANDIDATE'::character varying, 'ROLE_SUB_ADMIN'::character varying])::text[])))");
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS job_preferred_skills (job_id UUID NOT NULL, skill VARCHAR(255) NOT NULL, CONSTRAINT fk_job_preferred_skills_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE)");
            jdbcTemplate.execute("ALTER TABLE jobs DROP CONSTRAINT IF EXISTS jobs_experience_level_check");
            jdbcTemplate.execute("ALTER TABLE jobs ADD CONSTRAINT jobs_experience_level_check CHECK (experience_level IN ('FRESHER', 'JUNIOR', 'MID_LEVEL', 'SENIOR', 'LEAD', 'EXECUTIVE'))");
            // Multi-role migration: Ensure user_roles join table exists and backfill from users.role_id
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS user_roles (" +
                    "user_id UUID NOT NULL, " +
                    "role_id BIGINT NOT NULL, " +
                    "PRIMARY KEY (user_id, role_id), " +
                    "CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, " +
                    "CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE" +
                    ")");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles(role_id)");
            jdbcTemplate.execute("INSERT INTO user_roles (user_id, role_id) " +
                    "SELECT id, role_id FROM users " +
                    "WHERE role_id IS NOT NULL " +
                    "ON CONFLICT (user_id, role_id) DO NOTHING");
        } catch (Exception ex) {
            log.debug("[DataInitializer] Schema alignment notice: {}", ex.getMessage());
        }

        // 1. Initialize Essential Roles
        Role adminRole = createRoleIfNotExists(RoleType.ROLE_ADMIN, "System Administrator");
        Role subAdminRole = createRoleIfNotExists(RoleType.ROLE_SUB_ADMIN, "Sub Administrator");
        Role recruiterRole = createRoleIfNotExists(RoleType.ROLE_RECRUITER, "Recruiter");
        Role candidateRole = createRoleIfNotExists(RoleType.ROLE_CANDIDATE, "Candidate");

        // 2. Initialize Seed Administrator User (Only when explicitly enabled, password configured, and >= 12 chars)
        if (adminSeedEnabled) {
            if (adminSeedPassword == null || adminSeedPassword.isBlank()) {
                log.warn("[DataInitializer] Admin user seeding is enabled, but ADMIN_SEED_PASSWORD is not set. Skipping admin user creation.");
            } else if (adminSeedPassword.trim().length() < 12) {
                log.error("[DataInitializer] ADMIN_SEED_PASSWORD must be at least 12 characters long. Refusing to seed admin user.");
                throw new IllegalArgumentException("[DataInitializer] Insecure ADMIN_SEED_PASSWORD: Password must be at least 12 characters.");
            } else {
                userRepository.findByEmail("admin@vionsys.com").orElseGet(() -> {
                    User u = User.builder()
                            .firstName("System")
                            .lastName("Administrator")
                            .email("admin@vionsys.com")
                            .password(passwordEncoder.encode(adminSeedPassword.trim()))
                            .role(adminRole)
                            .enabled(true)
                            .accountNonLocked(true)
                            .build();
                    log.info("[DataInitializer] Initialized system administrator account (admin@vionsys.com).");
                    return userRepository.save(u);
                });
            }
        } else {
            log.info("[DataInitializer] Admin user seeding is disabled (hireai.admin.seed.enabled=false).");
        }

        // 3. Initialize Demo Users & Seeded Conversations (Dev Profile Only, Password from Property)
        if (environment.acceptsProfiles(Profiles.of("dev"))) {
            if (devDemoPassword == null || devDemoPassword.isBlank()) {
                log.info("[DataInitializer] 'dev' profile active, but hireai.dev.demo-password is not set. Skipping demo users.");
                return;
            }

            log.info("[DataInitializer] 'dev' profile active: seeding development demo accounts...");
            String encodedDemoPassword = passwordEncoder.encode(devDemoPassword.trim());

            User recruiter = userRepository.findByEmail("jordan.kim@vionsys.com").orElseGet(() -> {
                User u = User.builder()
                        .firstName("Jordan")
                        .lastName("Kim")
                        .email("jordan.kim@vionsys.com")
                        .password(encodedDemoPassword)
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
                        .password(encodedDemoPassword)
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
                        .password(encodedDemoPassword)
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
                        .password(encodedDemoPassword)
                        .role(candidateRole)
                        .enabled(true)
                        .accountNonLocked(true)
                        .build();
                return userRepository.save(u);
            });

            // 4. Initialize Persistent Database Conversations and Messages if empty
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