package com.ktpm.parking.api.dto;

/**
 * DTO trả về kết quả tiếp nhận xe vào bãi thành công.
 * Khớp với EntryResponse schema trong đặc tả OpenAPI.
 */
public record EntryResponse(
        // Trạng thái quyết định của hệ thống (luôn là "ACCEPTED" khi thành công)
        String status,

        // ID của phiên gửi xe vừa được tạo
        Long sessionId,

        // Mã chỗ đỗ được hệ thống tự động phân bổ (ví dụ: A01, B05)
        String assignedSpot
) {
    /**
     * Phương thức tiện ích để khởi tạo EntryResponse với status chuẩn ACCEPTED.
     *
     * @param sessionId    mã phiên gửi xe
     * @param assignedSpot mã vị trí đỗ được cấp
     * @return đối tượng EntryResponse hoàn chỉnh
     */
    public static EntryResponse of(Long sessionId, String assignedSpot) {
        return new EntryResponse("ACCEPTED", sessionId, assignedSpot);
    }
}
