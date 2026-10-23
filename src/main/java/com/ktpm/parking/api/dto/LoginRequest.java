package com.ktpm.parking.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Tên đăng nhập không được để trống")
        @Size(max = 100, message = "Tên đăng nhập tối đa 100 ký tự")
        String username,

        @NotBlank(message = "Mật khẩu không được để trống")
        String password
) {
}
