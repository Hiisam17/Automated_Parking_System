package com.ktpm.parking.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình OpenAPI / Swagger UI cho hệ thống Automated Parking System.
 * Thiết lập thông tin tài liệu API và các cơ chế xác thực bảo mật (Security Schemes)
 * gồm JWT Bearer Token (Admin) và API Key trong Header (Gate Simulator) để có thể test trực tiếp trên giao diện Swagger UI.
 */
@Configuration
public class OpenApiConfiguration {

    // Tên scheme xác thực cho Admin dùng JWT
    public static final String ADMIN_BEARER_AUTH = "AdminBearer";

    // Tên scheme xác thực cho Gate Simulator dùng API Key
    public static final String GATE_API_KEY_AUTH = "GateApiKey";

    /**
     * Khởi tạo cấu hình OpenAPI tùy chỉnh với thông tin hệ thống và nút Authorize trên giao diện Swagger UI.
     */
    @Bean
    public OpenAPI automatedParkingOpenAPI() {
        return new OpenAPI()
                // 1. Thông tin chung của tài liệu API
                .info(new Info()
                        .title("Automated Parking System — REST API")
                        .description("Tài liệu đặc tả các giao diện lập trình ứng dụng (REST API) cho hệ thống bãi đỗ xe tự động. " +
                                "Bao gồm các endpoint cho Cổng xe mô phỏng (Gate Simulator) và Quản trị viên (Admin).")
                        .version("1.0.0")
                        .contact(new Contact().name("KTPM - Automated Parking System Team")))
                // 2. Khai báo các cơ chế bảo mật (Security Schemes) trong Components
                .components(new Components()
                        // Cơ chế xác thực 1: JWT Bearer cho Admin
                        .addSecuritySchemes(ADMIN_BEARER_AUTH, new SecurityScheme()
                                .name(ADMIN_BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Nhập Access Token (chuỗi JWT) sau khi đăng nhập thành công từ API /api/v1/auth/login để gọi các API Admin."))
                        // Cơ chế xác thực 2: X-Gate-Api-Key cho Gate Simulator
                        .addSecuritySchemes(GATE_API_KEY_AUTH, new SecurityScheme()
                                .name("X-Gate-Api-Key")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("Nhập giá trị API Key của Gate Simulator để kiểm thử các API xe vào/ra /api/v1/gates/**.")))
                // 3. Kích hoạt nút Authorize trên Swagger UI
                .addSecurityItem(new SecurityRequirement().addList(ADMIN_BEARER_AUTH))
                .addSecurityItem(new SecurityRequirement().addList(GATE_API_KEY_AUTH));
    }
}
