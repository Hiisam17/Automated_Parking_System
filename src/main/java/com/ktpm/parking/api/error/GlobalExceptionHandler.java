package com.ktpm.parking.api.error;

import com.ktpm.parking.business.exception.BusinessException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Bắt lỗi sai thông tin đăng nhập (sai username hoặc sai password).
     * Trả về HTTP 401 Unauthorized kèm mã lỗi INVALID_CREDENTIALS theo đặc tả OpenAPI.
     */
    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiErrorResponse.of(
                "INVALID_CREDENTIALS",
                "Tên đăng nhập hoặc mật khẩu không đúng."
        ));
    }

    /**
     * Bắt lỗi chưa xác thực hoặc token không hợp lệ khi gọi API yêu cầu bảo mật.
     * Trả về HTTP 401 Unauthorized kèm mã lỗi UNAUTHORIZED.
     */
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiErrorResponse.of(
                "UNAUTHORIZED",
                "Yêu cầu xác thực hợp lệ để truy cập tài nguyên này."
        ));
    }

    /**
     * Bắt lỗi không đủ quyền truy cập (ví dụ Gate gọi vào Admin API).
     * Trả về HTTP 403 Forbidden kèm mã lỗi FORBIDDEN.
     */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiErrorResponse.of(
                "FORBIDDEN",
                "Bạn không có quyền thực hiện hành động này."
        ));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    ResponseEntity<ApiErrorResponse> handleValidation(Exception exception) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of(
                "VALIDATION_ERROR",
                exception.getMessage() != null && !exception.getMessage().isBlank()
                        ? exception.getMessage()
                        : "Dữ liệu đầu vào, query hoặc định dạng tham số không hợp lệ."
        ));
    }

    /**
     * Bắt lỗi không tìm thấy tài nguyên cụ thể theo nghiệp vụ (ví dụ: SPOT_NOT_FOUND, SESSION_NOT_FOUND).
     * Trả về HTTP 404 Not Found kèm mã lỗi tương ứng theo đặc tả OpenAPI.
     */
    @ExceptionHandler(com.ktpm.parking.business.exception.ResourceNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleResourceNotFound(com.ktpm.parking.business.exception.ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiErrorResponse.of(
                exception.getCode(),
                exception.getMessage()
        ));
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiErrorResponse> handleNotFound(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiErrorResponse.of(
                "RESOURCE_NOT_FOUND",
                "Không tìm thấy tài nguyên được yêu cầu."
        ));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        LOGGER.error("Unexpected request processing error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiErrorResponse.of(
                "INTERNAL_ERROR",
                "Lỗi bất ngờ phía máy chủ; không tiết lộ thông tin nhạy cảm."
        ));
    }
}
