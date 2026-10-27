package com.ktpm.parking.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử đơn vị (Unit Test) cho cấu hình OpenApiConfiguration.
 * Đảm bảo các thông số tiêu đề, phiên bản và các cơ chế bảo mật (AdminBearer, GateApiKey) được cấu hình chuẩn.
 */
class OpenApiConfigurationTest {

    private final OpenApiConfiguration openApiConfiguration = new OpenApiConfiguration();

    @Test
    @DisplayName("Cấu hình OpenAPI có đầy đủ thông tin hệ thống và các cơ chế bảo mật cho Swagger UI")
    void shouldConfigureOpenApiWithInfoAndSecuritySchemes() {
        OpenAPI openAPI = openApiConfiguration.automatedParkingOpenAPI();

        // 1. Kiểm tra thông tin Info
        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Automated Parking System — REST API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0.0");

        // 2. Kiểm tra Security Scheme AdminBearer (JWT)
        assertThat(openAPI.getComponents()).isNotNull();
        SecurityScheme adminScheme = openAPI.getComponents().getSecuritySchemes().get(OpenApiConfiguration.ADMIN_BEARER_AUTH);
        assertThat(adminScheme).isNotNull();
        assertThat(adminScheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(adminScheme.getScheme()).isEqualTo("bearer");
        assertThat(adminScheme.getBearerFormat()).isEqualTo("JWT");

        // 3. Kiểm tra Security Scheme GateApiKey
        SecurityScheme gateScheme = openAPI.getComponents().getSecuritySchemes().get(OpenApiConfiguration.GATE_API_KEY_AUTH);
        assertThat(gateScheme).isNotNull();
        assertThat(gateScheme.getType()).isEqualTo(SecurityScheme.Type.APIKEY);
        assertThat(gateScheme.getIn()).isEqualTo(SecurityScheme.In.HEADER);
        assertThat(gateScheme.getName()).isEqualTo("X-Gate-Api-Key");

        // 4. Kiểm tra Security Requirements được áp dụng
        assertThat(openAPI.getSecurity()).isNotEmpty();
    }
}
