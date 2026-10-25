package com.vionsys.hireai.security.jwt;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.vionsys.hireai.security.CustomUserDetails;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    /**
     * Generate Access Token
     */
    public String generateAccessToken(UserDetails userDetails) {

        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;

        Map<String, Object> claims = new HashMap<>();

        claims.put("userId", customUserDetails.getId().toString());

        String activeRoleName = customUserDetails.getActiveRole() != null
                ? customUserDetails.getActiveRole().name()
                : (customUserDetails.getAuthorities().isEmpty()
                        ? ""
                        : customUserDetails.getAuthorities().iterator().next().getAuthority());

        claims.put("activeRole", activeRoleName);
        claims.put("role", activeRoleName);

        java.util.List<String> rolesList = customUserDetails.getRoles() != null && !customUserDetails.getRoles().isEmpty()
                ? customUserDetails.getRoles().stream().map(Enum::name).toList()
                : java.util.List.of(activeRoleName);
        claims.put("roles", rolesList);

        return buildToken(
                claims,
                userDetails,
                jwtProperties.getAccessTokenExpiration());
    }

    /**
     * Generate Refresh Token
     */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("jti", UUID.randomUUID().toString());
        return buildToken(
                claims,
                userDetails,
                jwtProperties.getRefreshTokenExpiration());
    }

    /**
     * Build JWT
     */
    private String buildToken(
            Map<String, Object> claims,
            UserDetails userDetails,
            long expiration) {

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extract Username (Email)
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract UserId
     */
    public UUID extractUserId(String token) {

        String id = extractAllClaims(token)
                .get("userId", String.class);

        return UUID.fromString(id);
    }

    /**
     * Extract Role (activeRole with fallback to role)
     */
    public String extractRole(String token) {
        return extractAllClaims(token)
                .get("role", String.class);
    }

    /**
     * Extract Active Role
     */
    public String extractActiveRole(String token) {
        String activeRole = extractAllClaims(token).get("activeRole", String.class);
        if (activeRole == null || activeRole.isBlank()) {
            return extractRole(token);
        }
        return activeRole;
    }

    /**
     * Extract Roles List
     */
    @SuppressWarnings("unchecked")
    public java.util.List<String> extractRoles(String token) {
        return extractAllClaims(token).get("roles", java.util.List.class);
    }

    /**
     * Extract Expiration
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic Claim Extractor
     */
    public <T> T extractClaim(
            String token,
            Function<Claims, T> claimsResolver) {

        Claims claims = extractAllClaims(token);

        return claimsResolver.apply(claims);
    }

    /**
     * Extract All Claims
     */
    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validate Token
     */
    public boolean isTokenValid(
            String token,
            UserDetails userDetails) {

        String username = extractUsername(token);
        if (!username.equals(userDetails.getUsername()) || isTokenExpired(token)) {
            return false;
        }

        if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
            return false;
        }

        String tokenActiveRole = extractActiveRole(token);
        if (tokenActiveRole != null && !tokenActiveRole.isBlank() && userDetails instanceof CustomUserDetails cud) {
            boolean roleValid = (cud.getActiveRole() != null && cud.getActiveRole().name().equals(tokenActiveRole))
                    || (cud.getRoles() != null && cud.getRoles().stream().anyMatch(r -> r.name().equals(tokenActiveRole)))
                    || (cud.getAuthorities() != null && cud.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(tokenActiveRole)));
            if (!roleValid) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check Expiration
     */
    public boolean isTokenExpired(String token) {

        return extractExpiration(token)
                .before(new Date());
    }

    /**
     * Secret Key
     */
    private SecretKey getSigningKey() {

        byte[] keyBytes =
                Decoders.BASE64.decode(jwtProperties.getSecret());

        return Keys.hmacShaKeyFor(keyBytes);
    }

}