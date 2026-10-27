package com.ktpm.parking.api.dto;

import com.ktpm.parking.business.domain.ParkingSession;
import com.ktpm.parking.business.domain.ParkingSessionStatus;
import com.ktpm.parking.business.domain.VehicleType;

import java.time.OffsetDateTime;

/**
 * DTO đại diện cho một phiên gửi xe trả về cho Admin (dùng cả cho danh sách và chi tiết).
 * Khớp với ParkingSession schema trong đặc tả.
 */
public record ParkingSessionResponse(
        // ID định danh duy nhất của phiên gửi xe
        Long id,

        // Biển số xe của phiên
        String licensePlate,

        // Loại phương tiện (MOTORBIKE hoặc CAR)
        VehicleType vehicleType,

        // ID của chỗ đỗ được phân bổ
        Long spotId,

        // Mã vị trí chỗ đỗ (ví dụ: A05, B01)
        String spotCode,

        // Thời điểm xe vào bãi (backend ghi nhận)
        OffsetDateTime checkInAt,

        // Thời điểm xe ra khỏi bãi (null nếu phiên còn ACTIVE)
        OffsetDateTime checkOutAt,

        // Trạng thái phiên gửi xe (ACTIVE hoặc COMPLETED)
        ParkingSessionStatus status
) {
    /**
     * Chuyển đổi từ đối tượng Domain ParkingSession sang ParkingSessionResponse DTO.
     * Ánh xạ checkInTime -> checkInAt, checkOutTime -> checkOutAt, parkingSpotId -> spotId, parkingSpotCode -> spotCode.
     */
    public static ParkingSessionResponse fromDomain(ParkingSession session) {
        return new ParkingSessionResponse(
                session.id(),
                session.licensePlate(),
                session.vehicleType(),
                session.parkingSpotId(),
                session.parkingSpotCode(),
                session.checkInTime(),
                session.checkOutTime(),
                session.status()
        );
    }
}
