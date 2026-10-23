package com.ktpm.parking.api.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
    public static LoginResponse of(String accessToken, long expiresIn) {
        return new LoginResponse(accessToken, "Bearer", expiresIn);
    }
}
