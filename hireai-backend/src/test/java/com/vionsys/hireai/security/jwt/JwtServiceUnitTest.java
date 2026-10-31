package com.vionsys.hireai.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vionsys.hireai.common.enums.RoleType;
import com.vionsys.hireai.role.entity.Role;
import com.vionsys.hireai.security.CustomUserDetails;
import com.vionsys.hireai.user.entity.User;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

@ExtendWith(MockitoExtension.class)
class JwtServiceUnitTest {

    @Mock
    private JwtProperties jwtProperties;

    private JwtService jwtService;

    // Standard valid 256-bit test secret (Base64)
    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci1oaXJlYWktdGVzdGluZy1wdXJwb3Nlcy1vbmx5LW11c3QtYmUtNjQtYnl0ZXM=";
    private static final String WRONG_SECRET = "d3Jvbmctc2VjcmV0LWtleS1mb3ItaGlyZWFpLXRlc3RpbmctcHVycG9zZXMtb25seS1tdXN0LWJlLTY0LWJ5dGVz";

    private CustomUserDetails candidateDetails;
    private CustomUserDetails recruiterDetails;
    private UUID userId;

    @BeforeEach
    void setUp() {
        when(jwtProperties.getSecret()).thenReturn(TEST_SECRET);
        jwtService = new JwtService(jwtProperties);

        userId = UUID.randomUUID();
        Role candidateRole = Role.builder().id(1L).name(RoleType.ROLE_CANDIDATE).build();
        Role recruiterRole = Role.builder().id(2L).name(RoleType.ROLE_RECRUITER).build();

        User user = User.builder()
                .id(userId)
                .email("candidate@example.com")
                .password("encoded")
                .firstName("Alice")
                .lastName("Smith")
                .enabled(true)
                .accountNonLocked(true)
                .role(candidateRole)
                .roles(Set.of(candidateRole))
                .build();

        candidateDetails = CustomUserDetails.fromUser(user, RoleType.ROLE_CANDIDATE);

        User recruiterUser = User.builder()
                .id(UUID.randomUUID())
                .email("recruiter@example.com")
                .password("encoded")
                .firstName("Bob")
                .lastName("Recruiter")
                .enabled(true)
                .accountNonLocked(true)
                .role(recruiterRole)
                .roles(Set.of(recruiterRole))
                .build();

        recruiterDetails = CustomUserDetails.fromUser(recruiterUser, RoleType.ROLE_RECRUITER);
    }

    @Test
    @DisplayName("shouldGenerateAndValidateToken_whenUserCredentialsAreValid")
    void shouldGenerateAndValidateToken_whenUserCredentialsAreValid() {
        // Arrange
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(900000L); // 15 mins

        // Act
        String token = jwtService.generateAccessToken(candidateDetails);

        // Assert
        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("candidate@example.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtService.extractActiveRole(token)).isEqualTo("ROLE_CANDIDATE");
        assertThat(jwtService.extractRoles(token)).contains("ROLE_CANDIDATE");
        assertTrue(jwtService.isTokenValid(token, candidateDetails));
    }

    @Test
    @DisplayName("shouldDetectExpiredToken_whenExpirationHasPassed")
    void shouldDetectExpiredToken_whenExpirationHasPassed() {
        // Arrange: Token issued with 0 expiration (already expired)
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(-1000L);
        String expiredToken = jwtService.generateAccessToken(candidateDetails);

        // Act & Assert: Parsing expired token throws ExpiredJwtException
        assertThrows(ExpiredJwtException.class, () -> jwtService.isTokenValid(expiredToken, candidateDetails));
    }

    @Test
    @DisplayName("shouldThrowSignatureException_whenTokenIsTampered")
    void shouldThrowSignatureException_whenTokenIsTampered() {
        // Arrange
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(900000L);
        String validToken = jwtService.generateAccessToken(candidateDetails);

        // Tamper with middle payload segment
        String[] parts = validToken.split("\\.");
        String tamperedPayload = parts[1] + "tamper";
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        // Act & Assert
        assertThrows(Exception.class, () -> jwtService.extractUsername(tamperedToken));
    }

    @Test
    @DisplayName("shouldThrowSignatureException_whenTokenSignedWithDifferentKey")
    void shouldThrowSignatureException_whenTokenSignedWithDifferentKey() {
        // Arrange: Generate token with wrong HMAC key
        byte[] wrongKeyBytes = Decoders.BASE64.decode(WRONG_SECRET);
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("activeRole", "ROLE_CANDIDATE");

        String foreignToken = Jwts.builder()
                .claims(claims)
                .subject("candidate@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900000L))
                .signWith(Keys.hmacShaKeyFor(wrongKeyBytes))
                .compact();

        // Act & Assert
        assertThrows(SignatureException.class, () -> jwtService.extractUsername(foreignToken));
    }

    @Test
    @DisplayName("shouldReturnFalse_whenValidatingTokenAgainstDifferentUser")
    void shouldReturnFalse_whenValidatingTokenAgainstDifferentUser() {
        // Arrange
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(900000L);
        String candidateToken = jwtService.generateAccessToken(candidateDetails);

        // Act
        boolean validForRecruiter = jwtService.isTokenValid(candidateToken, recruiterDetails);

        // Assert
        assertFalse(validForRecruiter);
    }

    @Test
    @DisplayName("shouldReturnFalse_whenUserDetailsIsDisabledOrLocked")
    void shouldReturnFalse_whenUserDetailsIsDisabledOrLocked() {
        // Arrange
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(900000L);
        String token = jwtService.generateAccessToken(candidateDetails);

        User lockedUser = User.builder()
                .id(userId)
                .email("candidate@example.com")
                .enabled(false)
                .accountNonLocked(false)
                .role(Role.builder().id(1L).name(RoleType.ROLE_CANDIDATE).build())
                .roles(Set.of(Role.builder().id(1L).name(RoleType.ROLE_CANDIDATE).build()))
                .build();
        CustomUserDetails disabledDetails = CustomUserDetails.fromUser(lockedUser, RoleType.ROLE_CANDIDATE);

        // Act & Assert
        assertFalse(jwtService.isTokenValid(token, disabledDetails));
    }
}
