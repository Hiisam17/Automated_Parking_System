package com.ktpm.parking.api.dto;

import com.ktpm.parking.business.domain.ParkingSpot;
import com.ktpm.parking.business.domain.ParkingSpotStatus;
import com.ktpm.parking.business.domain.VehicleType;

/**
 * DTO đại diện cho một chỗ đỗ xe trả về cho Admin.
 * Khớp với ParkingSpot schema trong đặc tả OpenAPI.
 */
public record ParkingSpotResponse(
        // ID định danh duy nhất của chỗ đỗ
        Long id,

        // Mã vị trí chỗ đỗ (ví dụ: A01, B05)
        String code,

        // Loại xe được phép đỗ (MOTORBIKE hoặc CAR)
        VehicleType vehicleType,

        // Trạng thái chiếm dụng (AVAILABLE hoặc OCCUPIED)
        ParkingSpotStatus status,

        // Cờ cho biết chỗ đỗ có đang trong trạng thái bảo trì hay không
        boolean inMaintenance
) {
    /**
     * Chuyển đổi từ đối tượng Domain ParkingSpot sang ParkingSpotResponse DTO.
     */
    public static ParkingSpotResponse fromDomain(ParkingSpot spot) {
        return new ParkingSpotResponse(
                spot.id(),
                spot.code(),
                spot.vehicleType(),
                spot.occupancyStatus(),
                spot.inMaintenance()
        );
    }
}
