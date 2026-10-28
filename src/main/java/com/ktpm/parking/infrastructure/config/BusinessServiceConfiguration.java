package com.ktpm.parking.infrastructure.config;

import com.ktpm.parking.business.port.AccessTokenIssuer;
import com.ktpm.parking.business.port.PasswordVerifier;
import com.ktpm.parking.business.repository.AdminAccountRepository;
import com.ktpm.parking.business.repository.ParkingSessionRepository;
import com.ktpm.parking.business.repository.ParkingSpotRepository;
import com.ktpm.parking.business.service.AuthService;
import com.ktpm.parking.business.service.ParkingSessionAdminService;
import com.ktpm.parking.business.service.ParkingSpotAdminService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class BusinessServiceConfiguration {

    @Bean
    AuthService authService(AdminAccountRepository adminAccounts,
                            PasswordVerifier passwordVerifier,
                            AccessTokenIssuer tokenIssuer) {
        return new AuthService(adminAccounts, passwordVerifier, tokenIssuer);
    }

    @Bean
    ParkingSpotAdminService parkingSpotAdminService(ParkingSpotRepository spots) {
        return new ParkingSpotAdminService(spots);
    }

    @Bean
    ParkingSessionAdminService parkingSessionAdminService(ParkingSessionRepository sessions) {
        return new ParkingSessionAdminService(sessions);
    }
}
