package com.vionsys.hireai.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
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

import com.vionsys.hireai.security.jwt.JwtAccessDeniedHandler;
import com.vionsys.hireai.security.jwt.JwtAuthenticationEntryPoint;
import com.vionsys.hireai.security.jwt.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final com.vionsys.hireai.security.ratelimit.RateLimitingFilter rateLimitingFilter;

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

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
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        configuration.setAllowedOrigins(origins);
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

                // Return 401 when authentication fails & 403 when access is denied
                .exceptionHandling(exception ->
                        exception
                                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                                .accessDeniedHandler(jwtAccessDeniedHandler))

                .authorizeHttpRequests(auth -> auth

                        // =====================================================
                        // PUBLIC APIs
                        // =====================================================

                        .requestMatchers("/auth/**", "/error", "/ws/chat/**")
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

                        // Public Profile Photos & Static Image Assets (enables browser <img> tag rendering without JWT headers)
                        .requestMatchers(HttpMethod.GET,
                                "/candidates/*/profile/photo",
                                "/recruiter/profile/*/photo",
                                "/users/*/avatar",
                                "/uploads/profile-photos/**")
                        .permitAll()

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

                        .requestMatchers("/users/**", "/api/v1/users/**", "/api/users/**", "/test")
                        .authenticated()

                        .anyRequest()
                        .authenticated())

                // JWT filter must run before Spring's default
                // username/password authentication filter
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(
                        rateLimitingFilter,
                        JwtAuthenticationFilter.class);

        return http.build();
    }
}