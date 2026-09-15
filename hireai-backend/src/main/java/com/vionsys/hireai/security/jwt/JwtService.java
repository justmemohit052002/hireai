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

        claims.put(
                "role",
                customUserDetails.getAuthorities()
                        .iterator()
                        .next()
                        .getAuthority());

        return buildToken(
                claims,
                userDetails,
                jwtProperties.getAccessTokenExpiration());
    }

    /**
     * Generate Refresh Token
     */
    public String generateRefreshToken(UserDetails userDetails) {

        return buildToken(
                new HashMap<>(),
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
     * Extract Role
     */
    public String extractRole(String token) {
        return extractAllClaims(token)
                .get("role", String.class);
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

        return username.equals(userDetails.getUsername())
                && !isTokenExpired(token);
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