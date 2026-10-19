package com.ktpm.parking.infrastructure.persistence.mapper;

import com.ktpm.parking.business.domain.ParkingSession;
import com.ktpm.parking.infrastructure.persistence.entity.ParkingSessionEntity;
import com.ktpm.parking.infrastructure.persistence.entity.ParkingSpotEntity;

public final class ParkingSessionMapper {

    private ParkingSessionMapper() {
    }

    public static ParkingSession toDomain(ParkingSessionEntity entity) {
        return new ParkingSession(
                entity.getId(),
                entity.getLicensePlate(),
                entity.getVehicleType(),
                entity.getParkingSpot().getId(),
                entity.getParkingSpot().getCode(),
                entity.getCheckInTime(),
                entity.getCheckOutTime(),
                entity.getStatus()
        );
    }

    public static ParkingSessionEntity toEntity(ParkingSession domain, ParkingSpotEntity parkingSpot) {
        return new ParkingSessionEntity(
                domain.id(),
                domain.licensePlate(),
                domain.vehicleType(),
                parkingSpot,
                domain.checkInTime(),
                domain.checkOutTime(),
                domain.status()
        );
    }
}
