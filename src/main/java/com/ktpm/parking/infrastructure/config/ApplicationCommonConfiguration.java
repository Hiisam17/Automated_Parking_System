package com.ktpm.parking.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Cấu hình các Bean dùng chung cho toàn bộ hệ thống.
 */
@Configuration
public class ApplicationCommonConfiguration {

    /**
     * Cung cấp Bean Clock theo múi giờ chuẩn UTC để toàn bộ hệ thống
     * ghi nhận thời gian vào/ra và thời gian bảo trì một cách nhất quán.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
