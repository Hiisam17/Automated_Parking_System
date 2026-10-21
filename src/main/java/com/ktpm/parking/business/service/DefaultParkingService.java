package com.ktpm.parking.business.service;

import java.util.Objects;

public final class DefaultParkingService implements ParkingService {

    private final CheckInService checkInService;
    private final CheckOutService checkOutService;

    public DefaultParkingService(CheckInService checkInService, CheckOutService checkOutService) {
        this.checkInService = Objects.requireNonNull(checkInService);
        this.checkOutService = Objects.requireNonNull(checkOutService);
    }

    @Override
    public GateResult checkIn(CheckInCommand command) {
        return checkInService.checkIn(command);
    }

    @Override
    public GateResult checkOut(CheckOutCommand command) {
        return checkOutService.checkOut(command);
    }
}
