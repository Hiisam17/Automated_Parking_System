package com.ktpm.parking.infrastructure.security;

import com.ktpm.parking.business.port.PasswordVerifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public final class BcryptPasswordVerifier implements PasswordVerifier {

    private final PasswordEncoder passwordEncoder;

    public BcryptPasswordVerifier(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
