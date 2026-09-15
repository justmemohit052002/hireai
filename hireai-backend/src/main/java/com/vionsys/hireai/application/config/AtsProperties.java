package com.vionsys.hireai.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ats")
public class AtsProperties {

    /**
     * Threshold (0-100) above which applications are automatically SHORTLISTED.
     */
    private int shortlistThreshold = 70;

    private LlmProperties llm = new LlmProperties();

    @Getter
    @Setter
    public static class LlmProperties {
        private boolean enabled = false;
        private String serviceUrl = "http://localhost:8000/api/v1/ats/score";
        private int timeoutMs = 5000;
    }
}
