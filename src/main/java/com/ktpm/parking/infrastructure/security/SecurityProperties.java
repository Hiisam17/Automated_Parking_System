package com.ktpm.parking.infrastructure.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        @NotBlank @Size(min = 16) String gateApiKey,
        @NotBlank @Size(min = 32) String jwtSecret,
        @Min(1) long jwtExpirationSeconds
) {
}
