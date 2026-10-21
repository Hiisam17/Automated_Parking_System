package com.ktpm.parking.business.repository;

import com.ktpm.parking.business.domain.ParkingSession;
import com.ktpm.parking.business.domain.VehicleType;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface ParkingSessionWorkflowRepository {

    boolean existsActiveByLicensePlate(String licensePlateKey);

    Optional<ParkingSession> lockActiveByLicensePlate(String licensePlateKey);

    ParkingSession createActive(
            String licensePlate,
            String licensePlateKey,
            VehicleType vehicleType,
            Long spotId,
            OffsetDateTime checkInTime
    );

    boolean complete(Long sessionId, OffsetDateTime checkOutTime);
}
