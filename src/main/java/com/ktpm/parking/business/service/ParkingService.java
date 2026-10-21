package com.ktpm.parking.business.service;

public interface ParkingService {

    GateResult checkIn(CheckInCommand command);

    GateResult checkOut(CheckOutCommand command);
}
