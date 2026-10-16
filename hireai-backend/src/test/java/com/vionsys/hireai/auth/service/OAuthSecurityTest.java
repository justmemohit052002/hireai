package com.vionsys.hireai.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vionsys.hireai.auth.dto.OAuthRequest;
import com.vionsys.hireai.auth.dto.OAuthUserInfo;
import com.vionsys.hireai.exception.InvalidTokenException;

class OAuthSecurityTest {

    @Test
    void whenMockTokensDisabled_rejectsMockGoogleToken() {
        OAuthService oAuthService = new OAuthService(new ObjectMapper());
        ReflectionTestUtils.setField(oAuthService, "allowMockTokens", false);

        OAuthRequest request = OAuthRequest.builder()
                .token("mock-token-xyz")
                .email("attacker@example.com")
                .build();

        assertThrows(InvalidTokenException.class, () ->
                oAuthService.extractUserInfo("GOOGLE", request)
        );
    }

    @Test
    void whenMockTokensDisabled_rejectsMockLinkedInToken() {
        OAuthService oAuthService = new OAuthService(new ObjectMapper());
        ReflectionTestUtils.setField(oAuthService, "allowMockTokens", false);

        OAuthRequest request = OAuthRequest.builder()
                .token("mock-linkedin-token")
                .email("attacker@example.com")
                .build();

        assertThrows(InvalidTokenException.class, () ->
                oAuthService.extractUserInfo("LINKEDIN", request)
        );
    }

    @Test
    void whenMockTokensEnabled_allowsMockTokenOnlyInDev() {
        OAuthService oAuthService = new OAuthService(new ObjectMapper());
        ReflectionTestUtils.setField(oAuthService, "allowMockTokens", true);

        OAuthRequest request = OAuthRequest.builder()
                .token("mock-google-123")
                .email("dev@example.com")
                .firstName("Dev")
                .lastName("User")
                .build();

        OAuthUserInfo info = oAuthService.extractUserInfo("GOOGLE", request);
        assertEquals("dev@example.com", info.getEmail());
        assertTrue(info.getProviderId().startsWith("mock-"));
    }
}
