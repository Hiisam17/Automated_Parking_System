package com.ktpm.parking.infrastructure.persistence.springdata;

import com.ktpm.parking.infrastructure.persistence.entity.ParkingSpotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingSpotJpaRepository extends JpaRepository<ParkingSpotEntity, Long> {
}
