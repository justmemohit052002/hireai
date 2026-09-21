package com.vionsys.hireai.security.ratelimit;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vionsys.hireai.exception.ErrorResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter responsible for enforcing tiered rate limits across:
 * 1. Strict limits on authentication routes (per-IP and per-account with exponential backoff).
 * 2. Moderate limits on public endpoints (per-IP).
 * 3. Lenient/higher capacity limits on authenticated user actions (per-user).
 */
@Slf4j
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final RateLimiter rateLimiter;
    private final AccountBackoffManager accountBackoffManager;
    private final ObjectMapper objectMapper;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("\"email\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Bypass CORS preflight requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String clientIp = resolveClientIp(request);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        // =========================================================================
        // TIER 1: AUTHENTICATED USER ACTIONS (Higher allowance per user)
        // =========================================================================
        if (isAuthenticated) {
            String username = authentication.getName();
            String userKey = "user:" + username;
            RateLimitProperties.AuthenticatedProperties authConfig = properties.getAuthenticated();

            RateLimitResult result = rateLimiter.tryConsume(userKey, authConfig.getCapacity(), authConfig.getWindowSeconds());
            attachRateLimitHeaders(response, result);

            if (!result.isAllowed()) {
                log.warn("Rate limit exceeded for authenticated user '{}': IP={}", username, clientIp);
                writeRateLimitResponse(
                        response,
                        request,
                        result.getRetryAfterSeconds(),
                        "Too many requests for your account. Please wait " + result.getRetryAfterSeconds() + " seconds."
                );
                return;
            }

            filterChain.doFilter(request, response);
            return;
        }

        // =========================================================================
        // TIER 2: STRICT AUTHENTICATION ROUTES (Per-IP & Per-Account with Backoff)
        // =========================================================================
        if (isAuthRoute(path)) {
            // A) Per-IP Rate Limit on Auth Routes
            RateLimitProperties.IpProperties ipConfig = properties.getAuth().getIp();
            String ipKey = "auth:ip:" + clientIp;
            RateLimitResult ipResult = rateLimiter.tryConsume(ipKey, ipConfig.getCapacity(), ipConfig.getWindowSeconds());
            attachRateLimitHeaders(response, ipResult);

            if (!ipResult.isAllowed()) {
                log.warn("Strict auth rate limit exceeded for IP {}: URI={}", clientIp, path);
                writeRateLimitResponse(
                        response,
                        request,
                        ipResult.getRetryAfterSeconds(),
                        "Too many authentication attempts from this IP. Please try again in " + ipResult.getRetryAfterSeconds() + " seconds."
                );
                return;
            }

            // B) Per-Account Exponential Backoff Check
            HttpServletRequest requestToPass = request;
            String accountEmail = null;

            if ("POST".equalsIgnoreCase(request.getMethod()) && isJsonRequest(request)) {
                CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
                requestToPass = cachedRequest;
                accountEmail = extractEmailFromBody(cachedRequest.getBodyAsString());
            }

            if (accountEmail == null || accountEmail.isBlank()) {
                accountEmail = request.getParameter("email");
            }

            if (accountEmail != null && !accountEmail.isBlank()) {
                AccountBackoffManager.BackoffResult backoff = accountBackoffManager.checkBackoff(accountEmail);
                if (backoff.isBlocked()) {
                    log.warn("Authentication request blocked by exponential backoff for account '{}' from IP {}", accountEmail, clientIp);
                    writeRateLimitResponse(
                            response,
                            request,
                            backoff.getRemainingCooldownSeconds(),
                            "Too many failed login attempts for this account. Exponential backoff active. Please wait "
                                    + backoff.getRemainingCooldownSeconds() + " seconds before retrying."
                    );
                    return;
                }
            }

            filterChain.doFilter(requestToPass, response);
            return;
        }

        // =========================================================================
        // TIER 3: MODERATE PUBLIC ENDPOINTS (Per-IP)
        // =========================================================================
        RateLimitProperties.PublicProperties publicConfig = properties.getPublicEndpoints();
        String publicIpKey = "public:ip:" + clientIp;
        RateLimitResult publicResult = rateLimiter.tryConsume(publicIpKey, publicConfig.getCapacity(), publicConfig.getWindowSeconds());
        attachRateLimitHeaders(response, publicResult);

        if (!publicResult.isAllowed()) {
            log.warn("Public endpoint rate limit exceeded for IP {}: URI={}", clientIp, path);
            writeRateLimitResponse(
                    response,
                    request,
                    publicResult.getRetryAfterSeconds(),
                    "Too many requests to public endpoints. Please wait " + publicResult.getRetryAfterSeconds() + " seconds."
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAuthRoute(String path) {
        return path != null && (
                path.startsWith("/auth/login")
                        || path.startsWith("/auth/register")
                        || path.startsWith("/auth/forgot-password")
                        || path.startsWith("/auth/reset-password")
                        || path.startsWith("/auth/refresh")
        );
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE);
    }

    private String extractEmailFromBody(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.hasNonNull("email")) {
                return node.get("email").asText().trim();
            }
        } catch (Exception ignored) {
            // Fallback to regex extraction
            Matcher matcher = EMAIL_PATTERN.matcher(body);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        return null;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String[] headerCandidates = {
                "X-Forwarded-For",
                "X-Real-IP",
                "CF-Connecting-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP"
        };

        for (String header : headerCandidates) {
            String ipList = request.getHeader(header);
            if (ipList != null && !ipList.isBlank() && !"unknown".equalsIgnoreCase(ipList)) {
                return ipList.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    private void attachRateLimitHeaders(HttpServletResponse response, RateLimitResult result) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.getResetEpochSeconds()));
    }

    private void writeRateLimitResponse(
            HttpServletResponse response,
            HttpServletRequest request,
            long retryAfterSeconds,
            String message) throws IOException {

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));

        ErrorResponse errorResponse = new ErrorResponse(
                false,
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                message,
                request.getRequestURI()
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        response.getWriter().flush();
    }
}
