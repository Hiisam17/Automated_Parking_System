package com.ktpm.parking.business.service;

public record CheckOutCommand(
        String eventId,
        String gateId,
        String licensePlate
) {
}
