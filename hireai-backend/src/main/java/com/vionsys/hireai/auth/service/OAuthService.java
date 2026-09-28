package com.vionsys.hireai.auth.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vionsys.hireai.auth.dto.OAuthRequest;
import com.vionsys.hireai.auth.dto.OAuthUserInfo;
import com.vionsys.hireai.exception.InvalidTokenException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * Extracts and validates user profile from Google or LinkedIn OAuth tokens.
     */
    public OAuthUserInfo extractUserInfo(String provider, OAuthRequest request) {
        String upperProvider = provider.toUpperCase().trim();

        if ("GOOGLE".equals(upperProvider)) {
            return verifyGoogleToken(request);
        } else if ("LINKEDIN".equals(upperProvider)) {
            return verifyLinkedInToken(request);
        } else {
            throw new InvalidTokenException("Unsupported OAuth provider: " + provider);
        }
    }

    @org.springframework.beans.factory.annotation.Value("${google.oauth.client-id:}")
    private String configuredGoogleClientId;

    private OAuthUserInfo verifyGoogleToken(OAuthRequest request) {
        String token = request.getToken() != null ? request.getToken().trim() : "";

        // Simulated / development mode token
        if (token.startsWith("mock-") || token.startsWith("simulated-") || "demo-token".equalsIgnoreCase(token)) {
            log.info("Processing simulated Google OAuth token for: {}", request.getEmail());
            return buildMockUserInfo(request, "GOOGLE");
        }

        try {
            HttpRequest httpRequest;
            if (token.startsWith("ya29.") || !token.contains(".")) {
                // Google Access Token flow (via userinfo endpoint)
                httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://www.googleapis.com/oauth2/v3/userinfo"))
                        .header("Authorization", "Bearer " + token)
                        .timeout(Duration.ofSeconds(6))
                        .GET()
                        .build();
            } else {
                // Google ID Token / JWT flow (via tokeninfo endpoint)
                httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + token))
                        .timeout(Duration.ofSeconds(6))
                        .GET()
                        .build();
            }

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(response.body());
                String email = json.hasNonNull("email") ? json.get("email").asText() : null;
                boolean emailVerified = json.hasNonNull("email_verified") && json.get("email_verified").asBoolean(true);

                if (email == null || email.isBlank()) {
                    throw new InvalidTokenException("Google OAuth token does not contain a valid email.");
                }

                String firstName = json.hasNonNull("given_name") ? json.get("given_name").asText() : "Google";
                String lastName = json.hasNonNull("family_name") ? json.get("family_name").asText() : "User";
                String avatarUrl = json.hasNonNull("picture") ? json.get("picture").asText() : null;
                String providerId = json.hasNonNull("sub") ? json.get("sub").asText() : null;

                return OAuthUserInfo.builder()
                        .email(email.toLowerCase().trim())
                        .firstName(firstName)
                        .lastName(lastName)
                        .avatarUrl(avatarUrl)
                        .providerId(providerId)
                        .provider("GOOGLE")
                        .emailVerified(emailVerified)
                        .build();
            } else {
                log.warn("Google token validation failed with status {}: {}", response.statusCode(), response.body());
                throw new InvalidTokenException("Failed to verify Google identity token. Status: " + response.statusCode());
            }
        } catch (InvalidTokenException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Error verifying Google token with remote API: {}", ex.getMessage());
            throw new InvalidTokenException("Unable to contact Google OAuth servers: " + ex.getMessage());
        }
    }

    private OAuthUserInfo verifyLinkedInToken(OAuthRequest request) {
        String token = request.getToken() != null ? request.getToken().trim() : "";

        // Simulated / development mode token
        if (token.startsWith("mock-") || token.startsWith("simulated-") || "demo-token".equalsIgnoreCase(token)) {
            log.info("Processing simulated LinkedIn OAuth token for: {}", request.getEmail());
            return buildMockUserInfo(request, "LINKEDIN");
        }

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.linkedin.com/v2/userinfo"))
                    .header("Authorization", "Bearer " + token)
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(response.body());
                String email = json.hasNonNull("email") ? json.get("email").asText() : null;

                if (email == null || email.isBlank()) {
                    throw new InvalidTokenException("LinkedIn profile does not have a verified email.");
                }

                String firstName = json.hasNonNull("given_name") ? json.get("given_name").asText() : "LinkedIn";
                String lastName = json.hasNonNull("family_name") ? json.get("family_name").asText() : "User";
                String avatarUrl = json.hasNonNull("picture") ? json.get("picture").asText() : null;
                String providerId = json.hasNonNull("sub") ? json.get("sub").asText() : null;

                return OAuthUserInfo.builder()
                        .email(email.toLowerCase().trim())
                        .firstName(firstName)
                        .lastName(lastName)
                        .avatarUrl(avatarUrl)
                        .providerId(providerId)
                        .provider("LINKEDIN")
                        .emailVerified(true)
                        .build();
            } else {
                log.warn("LinkedIn userinfo failed with status {}: {}", response.statusCode(), response.body());
                throw new InvalidTokenException("Failed to verify LinkedIn authentication token.");
            }
        } catch (InvalidTokenException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Error contacting LinkedIn API: {}", ex.getMessage());
            throw new InvalidTokenException("Unable to contact LinkedIn OAuth servers: " + ex.getMessage());
        }
    }

    private OAuthUserInfo buildMockUserInfo(OAuthRequest request, String provider) {
        String email = request.getEmail() != null && !request.getEmail().isBlank()
                ? request.getEmail().toLowerCase().trim()
                : (provider.toLowerCase() + ".user@example.com");

        String firstName = request.getFirstName() != null && !request.getFirstName().isBlank()
                ? request.getFirstName()
                : (provider.charAt(0) + provider.substring(1).toLowerCase());

        String lastName = request.getLastName() != null && !request.getLastName().isBlank()
                ? request.getLastName()
                : "Member";

        return OAuthUserInfo.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .avatarUrl(request.getAvatarUrl())
                .providerId("mock-" + provider.toLowerCase() + "-" + System.currentTimeMillis())
                .provider(provider)
                .emailVerified(true)
                .build();
    }
}
