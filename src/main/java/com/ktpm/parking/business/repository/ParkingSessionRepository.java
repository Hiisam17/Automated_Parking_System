package com.ktpm.parking.business.repository;

import com.ktpm.parking.business.domain.ParkingSession;

import java.util.Optional;

public interface ParkingSessionRepository {

    Optional<ParkingSession> findById(Long id);

    ParkingSession save(ParkingSession parkingSession);
}
