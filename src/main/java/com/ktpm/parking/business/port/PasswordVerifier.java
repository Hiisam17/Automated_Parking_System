package com.ktpm.parking.business.port;

public interface PasswordVerifier {

    boolean matches(String rawPassword, String encodedPassword);
}
