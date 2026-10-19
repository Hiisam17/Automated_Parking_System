package com.ktpm.parking.infrastructure.persistence.springdata;

import com.ktpm.parking.infrastructure.persistence.entity.ParkingSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingSessionJpaRepository extends JpaRepository<ParkingSessionEntity, Long> {
}
