package com.ktpm.parking.api.controller;

import com.ktpm.parking.api.dto.LoginRequest;
import com.ktpm.parking.api.dto.LoginResponse;
import com.ktpm.parking.infrastructure.security.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller tiếp nhận các yêu cầu xác thực người dùng.
 * Cung cấp endpoint đăng nhập cho quản trị viên (Admin).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * API Admin đăng nhập vào hệ thống.
     * Endpoint: POST /api/v1/auth/login
     * Quyền truy cập: Public (không yêu cầu token trước đó).
     *
     * @param request dữ liệu đăng nhập (username và password) được kiểm tra validation (@Valid)
     * @return HTTP 200 OK kèm theo JWT token nếu đăng nhập thành công
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
