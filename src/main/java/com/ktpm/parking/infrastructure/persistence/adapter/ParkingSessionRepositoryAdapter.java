package com.ktpm.parking.infrastructure.persistence.adapter;

import com.ktpm.parking.business.domain.ParkingSession;
import com.ktpm.parking.business.repository.ParkingSessionRepository;
import com.ktpm.parking.infrastructure.persistence.entity.ParkingSpotEntity;
import com.ktpm.parking.infrastructure.persistence.mapper.ParkingSessionMapper;
import com.ktpm.parking.infrastructure.persistence.springdata.ParkingSessionJpaRepository;
import com.ktpm.parking.infrastructure.persistence.springdata.ParkingSpotJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ParkingSessionRepositoryAdapter implements ParkingSessionRepository {

    private final ParkingSessionJpaRepository sessionJpaRepository;
    private final ParkingSpotJpaRepository spotJpaRepository;

    public ParkingSessionRepositoryAdapter(ParkingSessionJpaRepository sessionJpaRepository,
                                           ParkingSpotJpaRepository spotJpaRepository) {
        this.sessionJpaRepository = sessionJpaRepository;
        this.spotJpaRepository = spotJpaRepository;
    }

    @Override
    public Optional<ParkingSession> findById(Long id) {
        return sessionJpaRepository.findById(id).map(ParkingSessionMapper::toDomain);
    }

    @Override
    public java.util.List<ParkingSession> findAll() {
        return sessionJpaRepository.findAll().stream().map(ParkingSessionMapper::toDomain).toList();
    }

    @Override
    public ParkingSession save(ParkingSession parkingSession) {
        ParkingSpotEntity spot = spotJpaRepository.getReferenceById(parkingSession.parkingSpotId());
        return ParkingSessionMapper.toDomain(
                sessionJpaRepository.save(ParkingSessionMapper.toEntity(parkingSession, spot))
        );
    }
}
