package com.vionsys.hireai.security.ratelimit;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RateLimitingConfig {

    @Bean
    public RateLimitingFilter rateLimitingFilter(
            RateLimitProperties properties,
            RateLimiter rateLimiter,
            AccountBackoffManager accountBackoffManager,
            ObjectMapper objectMapper) {
        return new RateLimitingFilter(properties, rateLimiter, accountBackoffManager, objectMapper);
    }

    /**
     * Disables automatic registration with the root Servlet container (Tomcat).
     * RateLimitingFilter is registered directly in Spring Security's SecurityFilterChain
     * (in SecurityConfig.java) so that it executes with the user's Authentication context
     * and is not executed twice per request.
     */
    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilterRegistration(RateLimitingFilter filter) {
        FilterRegistrationBean<RateLimitingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
