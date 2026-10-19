package com.ktpm.parking.api.error;

import java.util.Map;

public record ApiErrorResponse(ApiError error) {

    public record ApiError(String code, String message, Map<String, Object> details) {
    }

    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse(new ApiError(code, message, null));
    }
}
