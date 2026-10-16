package com.vionsys.hireai.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Validates that mandatory security secrets are configured before application proceeds.
 * Fails fast with a clear, actionable error message if required secrets are missing or blank.
 */
@Slf4j
@Component
public class MandatoryPropertiesValidator {

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${ai-engine.internal-key}")
    private String aiEngineInternalKey;

    @PostConstruct
    public void validateRequiredSecrets() {
        List<String> missing = new ArrayList<>();

        if (datasourcePassword == null || datasourcePassword.trim().isEmpty()) {
            missing.add("spring.datasource.password (environment variable: SPRING_DATASOURCE_PASSWORD)");
        }
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            missing.add("jwt.secret (environment variable: JWT_SECRET)");
        }
        if (aiEngineInternalKey == null || aiEngineInternalKey.trim().isEmpty()) {
            missing.add("ai-engine.internal-key (environment variable: AI_ENGINE_INTERNAL_KEY)");
        }

        if (!missing.isEmpty()) {
            String errorMsg = "\n" +
                    "================================================================================\n" +
                    "APPLICATION FAILED TO START: MISSING MANDATORY SECURITY SECRETS\n" +
                    "================================================================================\n" +
                    "The following required configuration secrets are missing or empty:\n" +
                    String.join("\n", missing.stream().map(m -> "  * " + m).toList()) + "\n\n" +
                    "Action Required:\n" +
                    "Please configure the required environment variables in your deployment environment,\n" +
                    "system properties, or local .env file before starting the application.\n" +
                    "================================================================================\n";
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
    }
}
