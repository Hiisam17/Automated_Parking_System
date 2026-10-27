package com.ktpm.parking.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO nhận dữ liệu yêu cầu đăng nhập của quản trị viên (Admin).
 * Khớp với định nghĩa LoginRequest trong đặc tả OpenAPI.
 */
public record LoginRequest(
        // Tên đăng nhập của tài khoản quản trị viên (bắt buộc, tối đa 100 ký tự)
        @NotBlank(message = "Tên đăng nhập không được để trống")
        @Size(max = 100, message = "Tên đăng nhập tối đa 100 ký tự")
        String username,

        // Mật khẩu tài khoản quản trị viên (bắt buộc)
        @NotBlank(message = "Mật khẩu không được để trống")
        String password
) {
}
