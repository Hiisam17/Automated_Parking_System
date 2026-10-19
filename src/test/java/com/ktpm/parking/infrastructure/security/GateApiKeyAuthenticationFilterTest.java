package com.ktpm.parking.infrastructure.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class GateApiKeyAuthenticationFilterTest {

    private static final String API_KEY = "test-gate-api-key";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateValidGateApiKey() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v1/gates/entry");
        request.addHeader(GateApiKeyAuthenticationFilter.API_KEY_HEADER, API_KEY);
        var response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        new GateApiKeyAuthenticationFilter(API_KEY).doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_GATE");
    }

    @Test
    void shouldRejectInvalidGateApiKey() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v1/gates/entry");
        request.addHeader(GateApiKeyAuthenticationFilter.API_KEY_HEADER, "wrong-key");
        var response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        new GateApiKeyAuthenticationFilter(API_KEY).doFilter(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED");
    }
}
