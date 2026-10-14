package com.vionsys.hireai.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class MustChangePasswordFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            if (userDetails.isMustChangePassword()) {
                String uri = request.getRequestURI();
                if (isAllowedWithoutPasswordChange(uri, request.getMethod())) {
                    filterChain.doFilter(request, response);
                    return;
                }

                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("""
                    {
                        "success": false,
                        "code": "MUST_CHANGE_PASSWORD",
                        "message": "Password change required before accessing the platform.",
                        "mustChangePassword": true
                    }
                    """);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedWithoutPasswordChange(String uri, String method) {
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        if (uri == null) return false;
        String lower = uri.toLowerCase();
        return lower.endsWith("/change-password")
                || lower.startsWith("/auth/logout")
                || lower.startsWith("/auth/me")
                || lower.startsWith("/users/me")
                || lower.startsWith("/swagger-ui")
                || lower.startsWith("/v3/api-docs");
    }
}
