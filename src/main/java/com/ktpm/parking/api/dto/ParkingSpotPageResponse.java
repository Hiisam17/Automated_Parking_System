package com.ktpm.parking.api.dto;

import java.util.List;

/**
 * DTO trả về danh sách chỗ đỗ xe có phân trang cho Admin.
 * Khớp với ParkingSpotPage schema trong đặc tả OpenAPI.
 */
public record ParkingSpotPageResponse(
        // Danh sách các chỗ đỗ trong trang hiện tại
        List<ParkingSpotResponse> items,

        // Thông tin metadata phân trang
        PageMeta page
) {
}
