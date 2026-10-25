package com.vionsys.hireai.security.jwt;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.vionsys.hireai.common.enums.RoleType;
import com.vionsys.hireai.security.CustomUserDetails;
import com.vionsys.hireai.user.entity.User;
import com.vionsys.hireai.user.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
        } else if (request.getParameter("token") != null && !request.getParameter("token").isBlank()) {
            jwt = request.getParameter("token");
        }

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String email = jwtService.extractUsername(jwt);

            if (email != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                String activeRoleStr = jwtService.extractActiveRole(jwt);
                RoleType parsedRole = null;
                if (activeRoleStr != null && !activeRoleStr.isBlank()) {
                    try {
                        parsedRole = RoleType.valueOf(activeRoleStr);
                    } catch (Exception ignored) {}
                }
                final RoleType tokenActiveRole = parsedRole;

                User user = userRepository.findByEmail(email).orElse(null);

                if (user != null && !Boolean.TRUE.equals(user.getDeleted())) {
                    // Verify that tokenActiveRole is owned by the user in the database
                    boolean ownsRole = (tokenActiveRole == null) || user.hasRole(tokenActiveRole);

                    if (ownsRole) {
                        CustomUserDetails userDetails = CustomUserDetails.fromUser(user, tokenActiveRole);

                        if (jwtService.isTokenValid(jwt, userDetails)) {
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails,
                                            null,
                                            userDetails.getAuthorities());

                            authentication.setDetails(
                                    new WebAuthenticationDetailsSource()
                                            .buildDetails(request));

                            SecurityContextHolder
                                    .getContext()
                                    .setAuthentication(authentication);
                        } else {
                            SecurityContextHolder.clearContext();
                        }
                    } else {
                        // User does not own activeRole in database -> reject token
                        SecurityContextHolder.clearContext();
                    }
                } else {
                    SecurityContextHolder.clearContext();
                }
            }
        } catch (Exception ex) {
            // Malformed, invalid signature, or expired JWT -> clear security context and continue chain
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}