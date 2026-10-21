package com.ktpm.parking.business.service;

import com.ktpm.parking.business.domain.VehicleType;

public record CheckInCommand(
        String eventId,
        String gateId,
        String licensePlate,
        VehicleType vehicleType
) {
}
