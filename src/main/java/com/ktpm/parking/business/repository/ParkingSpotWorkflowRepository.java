package com.ktpm.parking.business.repository;

import com.ktpm.parking.business.domain.ParkingSpot;
import com.ktpm.parking.business.domain.VehicleType;

import java.util.Optional;

public interface ParkingSpotWorkflowRepository {

    Optional<ParkingSpot> lockFirstAvailable(VehicleType vehicleType);

    boolean markOccupied(Long spotId);

    boolean markAvailable(Long spotId);
}
