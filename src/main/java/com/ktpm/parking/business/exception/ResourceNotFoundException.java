package com.ktpm.parking.business.exception;

/**
 * Ngoại lệ nghiệp vụ khi không tìm thấy tài nguyên (tương ứng HTTP 404 Not Found).
 * Ví dụ: SPOT_NOT_FOUND, SESSION_NOT_FOUND theo đặc tả OpenAPI.
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String code, String message) {
        super(code, message);
    }
}
