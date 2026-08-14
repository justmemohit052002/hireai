package com.vionsys.hireai.security;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.vionsys.hireai.security.jwt.JwtAuthenticationEntryPoint;
import com.vionsys.hireai.security.jwt.JwtAuthenticationFilter;
import com.vionsys.hireai.security.jwt.JwtProperties;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                // Enable CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // REST API does not use browser sessions
                .csrf(AbstractHttpConfigurer::disable)

                // JWT-based authentication
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))

                // Return 401 when authentication fails
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(
                                jwtAuthenticationEntryPoint))

                .authorizeHttpRequests(auth -> auth

                        // =====================================================
                        // PUBLIC APIs
                        // =====================================================

                        .requestMatchers("/auth/**")
                        .permitAll()

                        // Swagger / OpenAPI
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**")
                        .permitAll()


                        // =====================================================
                        // CANDIDATE & SHARED JOB ENDPOINTS
                        // =====================================================

                        // Candidate apply to job
                        .requestMatchers(HttpMethod.POST, "/jobs/*/apply")
                        .hasRole("CANDIDATE")

                        // Browse open jobs
                        .requestMatchers(HttpMethod.GET, "/jobs/open")
                        .hasAnyRole("CANDIDATE", "RECRUITER", "ADMIN")


                        // =====================================================
                        // ADMIN APIs
                        // =====================================================

                        .requestMatchers("/admin/**")
                        .hasRole("ADMIN")


                        // =====================================================
                        // RECRUITER APIs
                        // =====================================================

                        .requestMatchers("/recruiter/**")
                        .hasAnyRole("RECRUITER", "ADMIN")

                        // Candidate management APIs (Recruiters and Admins)
                        .requestMatchers("/candidates", "/candidates/**")
                        .hasAnyRole("RECRUITER", "ADMIN")

                        // Job management APIs (Recruiters and Admins)
                        .requestMatchers("/jobs", "/jobs/**")
                        .hasAnyRole("RECRUITER", "ADMIN")


                        // =====================================================
                        // CANDIDATE APIs
                        // =====================================================

                        // Candidate's own profile/self-service & applications APIs
                        .requestMatchers("/candidate/**")
                        .hasRole("CANDIDATE")


                        // =====================================================
                        // JOB APPLICATIONS & USER SELF-SERVICE
                        // =====================================================

                        .requestMatchers("/applications/**")
                        .authenticated()


                        // =====================================================
                        // USER SELF-SERVICE & OTHER AUTHENTICATED APIS
                        // =====================================================

                        .requestMatchers("/users/**", "/test")
                        .authenticated()

                        .anyRequest()
                        .authenticated())

                // JWT filter must run before Spring's default
                // username/password authentication filter
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}