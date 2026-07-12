package com.studioos.server.auth;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Guards /internal/** endpoints called by trusted backend services (e.g. the
 * C++ Media Service), not end users. Distinct from JwtAuthFilter, which
 * handles user-facing JWT auth. Checks a shared secret header rather than
 * a user token.
 */
@Slf4j
@Component
public class InternalServiceAuthFilter extends OncePerRequestFilter {

    @Value("${internal.service.api-key}")
    private String expectedApiKey;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (request.getRequestURI().contains("/internal/")) {
            String providedKey = request.getHeader("X-Internal-Api-Key");
            if (!StringUtils.hasText(expectedApiKey)
                    || providedKey == null
                    || !providedKey.equals(expectedApiKey)) {
                log.warn("Rejected internal endpoint call with invalid/missing API key: {}", request.getRequestURI());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
