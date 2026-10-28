package com.ktpm.parking.business.service;

import java.util.Objects;

public record AuthenticationResult(String accessToken, String tokenType, long expiresIn) {

    public AuthenticationResult {
        Objects.requireNonNull(accessToken, "accessToken must not be null");
        Objects.requireNonNull(tokenType, "tokenType must not be null");
        if (expiresIn < 1) {
            throw new IllegalArgumentException("expiresIn must be positive");
        }
    }

    public static AuthenticationResult bearer(String accessToken, long expiresIn) {
        return new AuthenticationResult(accessToken, "Bearer", expiresIn);
    }
}
