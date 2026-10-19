package com.ktpm.parking.infrastructure.persistence.adapter;

import com.ktpm.parking.business.domain.GateEvent;
import com.ktpm.parking.business.repository.GateEventRepository;
import com.ktpm.parking.infrastructure.persistence.mapper.GateEventMapper;
import com.ktpm.parking.infrastructure.persistence.springdata.GateEventJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class GateEventRepositoryAdapter implements GateEventRepository {

    private final GateEventJpaRepository jpaRepository;

    public GateEventRepositoryAdapter(GateEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<GateEvent> findByEventId(String eventId) {
        return jpaRepository.findById(eventId).map(GateEventMapper::toDomain);
    }

    @Override
    public GateEvent save(GateEvent gateEvent) {
        return GateEventMapper.toDomain(jpaRepository.save(GateEventMapper.toEntity(gateEvent)));
    }
}
