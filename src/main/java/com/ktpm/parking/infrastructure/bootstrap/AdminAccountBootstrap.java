package com.ktpm.parking.infrastructure.bootstrap;

import com.ktpm.parking.business.domain.AdminAccount;
import com.ktpm.parking.business.repository.AdminAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(AdminBootstrapProperties.class)
public class AdminAccountBootstrap implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminAccountBootstrap.class);

    private final AdminAccountRepository adminAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminBootstrapProperties properties;

    public AdminAccountBootstrap(AdminAccountRepository adminAccountRepository,
                                 PasswordEncoder passwordEncoder,
                                 AdminBootstrapProperties properties) {
        this.adminAccountRepository = adminAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean usernameMissing = properties.username() == null || properties.username().isBlank();
        boolean passwordMissing = properties.password() == null || properties.password().isBlank();

        if (usernameMissing && passwordMissing) {
            LOGGER.info("Admin bootstrap is disabled because no credentials were provided");
            return;
        }
        if (usernameMissing || passwordMissing) {
            throw new IllegalStateException(
                    "ADMIN_BOOTSTRAP_USERNAME and ADMIN_BOOTSTRAP_PASSWORD must be provided together"
            );
        }

        String username = properties.username().trim();
        if (adminAccountRepository.findByUsername(username).isPresent()) {
            LOGGER.info("Admin bootstrap skipped because account '{}' already exists", username);
            return;
        }

        adminAccountRepository.save(new AdminAccount(
                null,
                username,
                passwordEncoder.encode(properties.password()),
                true
        ));
        LOGGER.info("Bootstrapped initial Admin account '{}'", username);
    }
}
