package com.ktpm.parking.api.dto;

import com.ktpm.parking.business.domain.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO nhận dữ liệu yêu cầu check-in xe vào bãi từ Gate Simulator.
 * Khớp với EntryRequest schema trong đặc tả OpenAPI.
 */
public record EntryRequest(
        // Mã duy nhất của sự kiện do Gate Simulator sinh ra (giữ nguyên khi retry)
        @NotBlank(message = "eventId không được để trống")
        @Size(max = 64, message = "eventId tối đa 64 ký tự")
        String eventId,

        // Mã định danh của cổng vào (ví dụ: IN-01)
        @NotBlank(message = "gateId không được để trống")
        @Size(max = 64, message = "gateId tối đa 64 ký tự")
        String gateId,

        // Biển số xe ghi nhận tại cổng (ví dụ: 29A-12345)
        @NotBlank(message = "licensePlate không được để trống")
        @Size(min = 4, max = 20, message = "Biển số xe phải từ 4 đến 20 ký tự")
        String licensePlate,

        // Loại phương tiện (MOTORBIKE hoặc CAR)
        @NotNull(message = "vehicleType không được để trống")
        VehicleType vehicleType
) {
}
