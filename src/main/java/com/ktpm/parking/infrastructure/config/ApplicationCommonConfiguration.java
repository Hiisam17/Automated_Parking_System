package com.ktpm.parking.infrastructure.config;

import com.ktpm.parking.business.service.CheckInCommand;
import com.ktpm.parking.business.service.CheckOutCommand;
import com.ktpm.parking.business.service.GateDecision;
import com.ktpm.parking.business.service.GateResult;
import com.ktpm.parking.business.service.ParkingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Cung cấp Bean dự phòng cho ParkingService trong giai đoạn phát triển song
     * song.
     * Bean này giúp ApplicationContext và Spring Boot Test (như
     * ApplicationIntegrationIT)
     * khởi động bình thường mà không bị lỗi thiếu Bean khi Member 1 & 2 chưa ghép
     * adapter thật.
     * Khi có implementation thật, Bean này sẽ tự động nhường chỗ
     * (@ConditionalOnMissingBean).
     */
    @Bean
    @ConditionalOnMissingBean(ParkingService.class)
    public ParkingService fallbackParkingService() {
        return new ParkingService() {
            @Override
            public GateResult checkIn(CheckInCommand command) {
                return GateResult.newResult(GateDecision.accepted(1L, "A01"));
            }

            @Override
            public GateResult checkOut(CheckOutCommand command) {
                return GateResult.newResult(GateDecision.accepted(1L, "A01"));
            }
        };
    }
}
