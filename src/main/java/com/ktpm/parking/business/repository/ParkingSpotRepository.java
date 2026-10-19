package com.ktpm.parking.business.repository;

import com.ktpm.parking.business.domain.ParkingSpot;

import java.util.List;
import java.util.Optional;

public interface ParkingSpotRepository {

    Optional<ParkingSpot> findById(Long id);

    List<ParkingSpot> findAll();

    ParkingSpot save(ParkingSpot parkingSpot);
}
