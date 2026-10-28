package com.ktpm.parking.business.exception;

public final class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Tên đăng nhập hoặc mật khẩu không đúng.");
    }
}
