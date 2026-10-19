package com.ktpm.parking.business.domain;

public record ParkingSpot(
        Long id,
        String code,
        VehicleType vehicleType,
        ParkingSpotStatus occupancyStatus,
        boolean inMaintenance
) {
}
