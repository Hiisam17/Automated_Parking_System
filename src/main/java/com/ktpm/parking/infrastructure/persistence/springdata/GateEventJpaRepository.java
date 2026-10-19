package com.ktpm.parking.infrastructure.persistence.springdata;

import com.ktpm.parking.infrastructure.persistence.entity.GateEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GateEventJpaRepository extends JpaRepository<GateEventEntity, String> {
}
