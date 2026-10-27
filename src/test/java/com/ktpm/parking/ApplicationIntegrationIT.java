package com.ktpm.parking;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationIntegrationIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("automated_parking_test")
            .withUsername("parking_test")
            .withPassword("test-only-password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.security.gate-api-key", () -> "test-only-gate-api-key");
        registry.add("app.security.jwt-secret", () -> "test-only-jwt-secret-at-least-32-chars");
        registry.add("app.security.jwt-expiration-seconds", () -> 3600);
    }

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    Flyway flyway;

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void applicationContextStarts() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void flywayCreatesSchemaAndSeedsParkingSpots() {
        Integer tableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN (
                    'parking_spots', 'parking_sessions', 'gate_events', 'admin_accounts'
                  )
                """, Integer.class);
        Integer spotCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM parking_spots",
                Integer.class
        );

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("3");
        assertThat(tableCount).isEqualTo(4);
        assertThat(spotCount).isEqualTo(30);
    }

    @Test
    void exposesHealthAndOpenApiContract() {
        var healthResponse = restTemplate.getForEntity("/actuator/health", String.class);
        var openApiResponse = restTemplate.getForEntity("/openapi.yaml", String.class);
        var swaggerResponse = restTemplate.getForEntity("/swagger-ui.html", String.class);
        var missingResponse = restTemplate.getForEntity("/swagger-ui/missing-resource.js", String.class);

        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(healthResponse.getBody()).contains("UP");
        assertThat(openApiResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(openApiResponse.getBody()).contains("Automated Parking System");
        assertThat(swaggerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(swaggerResponse.getBody()).contains("Swagger UI");
        assertThat(missingResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(missingResponse.getBody()).contains("RESOURCE_NOT_FOUND");
    }
}
