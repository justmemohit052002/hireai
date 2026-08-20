package com.vionsys.hireai.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ai-engine")
public class AiEngineProperties {

    /**
     * Base URL for the FastAPI AI Engine microservice (e.g. http://localhost:8000).
     */
    private String baseUrl = "http://localhost:8000";

    /**
     * HTTP request timeout in milliseconds.
     */
    private int timeoutMs = 30000;

    /**
     * Polling interval in milliseconds for async jobs (e.g. resume parsing).
     */
    private int pollingIntervalMs = 3000;

    /**
     * Maximum polling attempts before timeout.
     */
    private int pollingMaxAttempts = 30;
}
