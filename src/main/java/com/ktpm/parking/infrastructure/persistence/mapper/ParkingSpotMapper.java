package com.ktpm.parking.infrastructure.persistence.mapper;

import com.ktpm.parking.business.domain.ParkingSpot;
import com.ktpm.parking.infrastructure.persistence.entity.ParkingSpotEntity;

public final class ParkingSpotMapper {

    private ParkingSpotMapper() {
    }

    public static ParkingSpot toDomain(ParkingSpotEntity entity) {
        return new ParkingSpot(
                entity.getId(),
                entity.getCode(),
                entity.getVehicleType(),
                entity.getOccupancyStatus(),
                entity.isInMaintenance()
        );
    }

    public static ParkingSpotEntity toEntity(ParkingSpot domain) {
        return new ParkingSpotEntity(
                domain.id(),
                domain.code(),
                domain.vehicleType(),
                domain.occupancyStatus(),
                domain.inMaintenance()
        );
    }
}
