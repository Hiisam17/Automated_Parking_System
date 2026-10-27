package com.ktpm.parking.api.dto;

/**
 * DTO trả về kết quả xe ra khỏi bãi thành công.
 * Khớp với ExitResponse schema trong đặc tả OpenAPI.
 */
public record ExitResponse(
        // Trạng thái quyết định của hệ thống (luôn là "ACCEPTED" khi thành công)
        String status,

        // ID của phiên gửi xe vừa được hoàn tất
        Long sessionId,

        // Mã chỗ đỗ vừa được giải phóng về trạng thái trống (ví dụ: A01, B05)
        String releasedSpot
) {
    /**
     * Phương thức tiện ích để khởi tạo ExitResponse với status chuẩn ACCEPTED.
     *
     * @param sessionId    mã phiên gửi xe
     * @param releasedSpot mã vị trí đỗ vừa được giải phóng
     * @return đối tượng ExitResponse hoàn chỉnh
     */
    public static ExitResponse of(Long sessionId, String releasedSpot) {
        return new ExitResponse("ACCEPTED", sessionId, releasedSpot);
    }
}
