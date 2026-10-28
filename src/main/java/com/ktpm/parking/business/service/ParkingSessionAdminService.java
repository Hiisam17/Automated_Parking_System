package com.ktpm.parking.business.service;

import com.ktpm.parking.business.domain.ParkingSession;
import com.ktpm.parking.business.domain.ParkingSessionStatus;
import com.ktpm.parking.business.exception.ResourceNotFoundException;
import com.ktpm.parking.business.repository.ParkingSessionRepository;
import com.ktpm.parking.business.support.LicensePlateNormalizer;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ParkingSessionAdminService {

    private final ParkingSessionRepository sessions;

    public ParkingSessionAdminService(ParkingSessionRepository sessions) {
        this.sessions = Objects.requireNonNull(sessions);
    }

    public PageResult<ParkingSession> listParkingSessions(ParkingSessionStatus status,
                                                          String licensePlate,
                                                          OffsetDateTime from,
                                                          OffsetDateTime to,
                                                          int page,
                                                          int size) {
        validatePage(page, size);
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Mốc thời gian 'from' phải nhỏ hơn hoặc bằng 'to'.");
        }

        String searchKey = licensePlate == null || licensePlate.isBlank()
                ? null
                : LicensePlateNormalizer.toKey(licensePlate);
        List<ParkingSession> filtered = sessions.findAll().stream()
                .filter(session -> status == null || session.status() == status)
                .filter(session -> searchKey == null
                        || LicensePlateNormalizer.toKey(session.licensePlate()).contains(searchKey))
                .filter(session -> from == null || !session.checkInTime().isBefore(from))
                .filter(session -> to == null || !session.checkInTime().isAfter(to))
                .sorted(Comparator
                        .comparing(ParkingSession::checkInTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ParkingSession::id, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        long total = filtered.size();
        int fromIndex = (int) Math.min((long) page * size, total);
        int toIndex = (int) Math.min((long) fromIndex + size, total);
        return new PageResult<>(filtered.subList(fromIndex, toIndex), page, size, total);
    }

    public ParkingSession getParkingSession(Long id) {
        return sessions.findById(id).orElseThrow(() -> new ResourceNotFoundException(
                "SESSION_NOT_FOUND", "Không tìm thấy phiên gửi xe với ID: " + id
        ));
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("Tham số phân trang không hợp lệ.");
        }
    }
}
