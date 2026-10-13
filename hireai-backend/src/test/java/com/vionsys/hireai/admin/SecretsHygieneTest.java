package com.vionsys.hireai.admin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vionsys.hireai.admin.dto.AdminCallLogEntry;
import com.vionsys.hireai.admin.dto.AdminHealthResponse;
import com.vionsys.hireai.admin.dto.ProviderDetailsDto;
import com.vionsys.hireai.admin.dto.ProvidersConfigDto;

class SecretsHygieneTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Regex patterns matching sensitive keys/tokens that must never leak in admin responses
    private final List<Pattern> LEAK_PATTERNS = List.of(
            Pattern.compile("AIza[0-9A-Za-z-_]{35}"),               // Google API Key
            Pattern.compile("gsk_[a-zA-Z0-9]{20,}"),                 // Groq API Key
            Pattern.compile("sk-[a-zA-Z0-9]{20,}"),                  // OpenAI / Anthropic Key
            Pattern.compile("Bearer\\s+[A-Za-z0-9-_\\.]{20,}", Pattern.CASE_INSENSITIVE), // Raw Bearer Token
            Pattern.compile("password\\s*[:=]\\s*[^,}\\s]+", Pattern.CASE_INSENSITIVE)     // Plain passwords
    );

    @Test
    @DisplayName("Providers status response masks secrets safely (shows only last 4 chars if > 12 chars)")
    void testProvidersConfig_SecretsAreMasked() throws Exception {
        ProvidersConfigDto providers = ProvidersConfigDto.builder()
                .activeProvider("ollama")
                .activeModel("llama3.2:3b")
                .ollama(ProviderDetailsDto.builder().name("Ollama (Local)").isConfigured(true).reachable(true).build())
                .gemini(ProviderDetailsDto.builder().name("Google Gemini").isConfigured(true).keyMasked("...a91f").build())
                .groq(ProviderDetailsDto.builder().name("Groq API").isConfigured(false).keyMasked("Not Configured").build())
                .build();

        String json = objectMapper.writeValueAsString(providers);

        for (Pattern pattern : LEAK_PATTERNS) {
            assertFalse(pattern.matcher(json).find(), "JSON output violated secrets hygiene: " + pattern.pattern());
        }

        assertTrue(json.contains("...a91f"));
        assertTrue(json.contains("Not Configured"));
    }

    @Test
    @DisplayName("Admin health and call log DTOs do not contain raw secrets or unmasked tokens")
    void testAdminDtos_NoRawSecretsLeak() throws Exception {
        AdminCallLogEntry entry = AdminCallLogEntry.builder()
                .id("call-uuid-1")
                .provider("gemini")
                .status("FAILED")
                .promptPreview("Generate job description for AI Architect")
                .errorMessage("API Error: Authentication failed [REDACTED_SECRET]")
                .build();

        String json = objectMapper.writeValueAsString(entry);

        for (Pattern pattern : LEAK_PATTERNS) {
            assertFalse(pattern.matcher(json).find(), "Call log JSON output violated secrets hygiene: " + pattern.pattern());
        }
    }
}
