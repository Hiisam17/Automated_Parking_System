package com.ktpm.parking.infrastructure.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin-bootstrap")
public record AdminBootstrapProperties(String username, String password) {
}
