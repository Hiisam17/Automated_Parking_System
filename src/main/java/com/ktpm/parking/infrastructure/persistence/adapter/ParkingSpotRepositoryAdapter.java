package com.ktpm.parking.infrastructure.persistence.adapter;

import com.ktpm.parking.business.domain.ParkingSpot;
import com.ktpm.parking.business.repository.ParkingSpotRepository;
import com.ktpm.parking.infrastructure.persistence.mapper.ParkingSpotMapper;
import com.ktpm.parking.infrastructure.persistence.springdata.ParkingSpotJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ParkingSpotRepositoryAdapter implements ParkingSpotRepository {

    private final ParkingSpotJpaRepository jpaRepository;

    public ParkingSpotRepositoryAdapter(ParkingSpotJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<ParkingSpot> findById(Long id) {
        return jpaRepository.findById(id).map(ParkingSpotMapper::toDomain);
    }

    @Override
    public List<ParkingSpot> findAll() {
        return jpaRepository.findAll().stream().map(ParkingSpotMapper::toDomain).toList();
    }

    @Override
    public ParkingSpot save(ParkingSpot parkingSpot) {
        return ParkingSpotMapper.toDomain(jpaRepository.save(ParkingSpotMapper.toEntity(parkingSpot)));
    }
}
