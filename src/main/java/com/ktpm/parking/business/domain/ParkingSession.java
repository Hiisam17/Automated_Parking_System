package com.ktpm.parking.business.domain;

import java.time.OffsetDateTime;

public record ParkingSession(
        Long id,
        String licensePlate,
        VehicleType vehicleType,
        Long parkingSpotId,
        String parkingSpotCode,
        OffsetDateTime checkInTime,
        OffsetDateTime checkOutTime,
        ParkingSessionStatus status
) {
}
