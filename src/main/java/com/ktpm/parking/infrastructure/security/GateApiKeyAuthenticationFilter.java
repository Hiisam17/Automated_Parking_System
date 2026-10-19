package com.ktpm.parking.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

public class GateApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-Gate-Api-Key";

    private final byte[] configuredApiKey;

    public GateApiKeyAuthenticationFilter(String configuredApiKey) {
        this.configuredApiKey = configuredApiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/gates/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String providedApiKey = request.getHeader(API_KEY_HEADER);
        if (!matches(providedApiKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"error\":{\"code\":\"UNAUTHORIZED\","
                            + "\"message\":\"Gate API key is missing or invalid.\"}}"
            );
            return;
        }

        var authentication = new UsernamePasswordAuthenticationToken(
                "gate-simulator",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_GATE"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private boolean matches(String providedApiKey) {
        return providedApiKey != null && MessageDigest.isEqual(
                configuredApiKey,
                providedApiKey.getBytes(StandardCharsets.UTF_8)
        );
    }
}
