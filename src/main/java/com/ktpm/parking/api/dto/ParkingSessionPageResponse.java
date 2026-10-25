package com.ktpm.parking.api.dto;

import java.util.List;

/**
 * DTO trả về danh sách phiên gửi xe có phân trang cho Admin.
 * Khớp với ParkingSessionPage schema trong đặc tả OpenAPI.
 */
public record ParkingSessionPageResponse(
        // Danh sách các phiên gửi xe trong trang hiện tại
        List<ParkingSessionResponse> items,

        // Thông tin metadata phân trang
        PageMeta page
) {
}
