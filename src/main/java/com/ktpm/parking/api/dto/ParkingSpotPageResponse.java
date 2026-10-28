package com.ktpm.parking.api.dto;

import com.ktpm.parking.business.domain.ParkingSpot;
import com.ktpm.parking.business.service.PageResult;

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
    public static ParkingSpotPageResponse from(PageResult<ParkingSpot> result) {
        return new ParkingSpotPageResponse(
                result.items().stream().map(ParkingSpotResponse::fromDomain).toList(),
                PageMeta.from(result)
        );
    }
}
