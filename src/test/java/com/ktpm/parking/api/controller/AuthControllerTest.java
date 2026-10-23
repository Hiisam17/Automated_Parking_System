package com.ktpm.parking.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktpm.parking.api.dto.LoginRequest;
import com.ktpm.parking.api.dto.LoginResponse;
import com.ktpm.parking.api.error.GlobalExceptionHandler;
import com.ktpm.parking.infrastructure.security.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnOkWhenLoginSucceeds() throws Exception {
        var response = LoginResponse.of("mocked-jwt-token", 3600);
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        var request = new LoginRequest("admin", "secret123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isEqualTo("mocked-jwt-token"))
                .andExpect(jsonPath("$.tokenType").isEqualTo("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isEqualTo(3600));
    }

    @Test
    void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Tên đăng nhập hoặc mật khẩu không đúng."));

        var request = new LoginRequest("admin", "wrongPassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").isEqualTo("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.error.message").isEqualTo("Tên đăng nhập hoặc mật khẩu không đúng."));
    }

    @Test
    void shouldReturn400WhenValidationFails() throws Exception {
        var request = new LoginRequest("", "");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").isEqualTo("VALIDATION_ERROR"));
    }
}
