package com.ktpm.parking.infrastructure.transaction;

import com.ktpm.parking.business.repository.ParkingSessionWorkflowRepository;
import com.ktpm.parking.business.repository.ParkingSpotWorkflowRepository;
import com.ktpm.parking.business.repository.ParkingUnitOfWork;
import com.ktpm.parking.business.service.ParkingService;
import com.ktpm.parking.infrastructure.persistence.adapter.GateEventWorkflowRepositoryAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class ParkingWorkflowConfiguration {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    Clock parkingClock() {
        return Clock.systemUTC();
    }

    @Bean
    ParkingService parkingService(ParkingSpotWorkflowRepository spots,
                                  ParkingSessionWorkflowRepository sessions,
                                  GateEventWorkflowRepositoryAdapter events,
                                  ParkingUnitOfWork unitOfWork, Clock clock) {
        return new PersistentParkingService(spots, sessions, events, unitOfWork, clock);
    }
}
