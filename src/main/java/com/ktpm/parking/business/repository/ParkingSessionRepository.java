package com.ktpm.parking.business.repository;

import com.ktpm.parking.business.domain.ParkingSession;

import java.util.Optional;

public interface ParkingSessionRepository {

    Optional<ParkingSession> findById(Long id);

    /**
     * Lấy toàn bộ danh sách phiên gửi xe phục vụ tra cứu cho Admin.
     */
    java.util.List<ParkingSession> findAll();

    ParkingSession save(ParkingSession parkingSession);
}
