package com.ktpm.parking.api.dto;

/**
 * DTO trả về kết quả đăng nhập thành công của quản trị viên.
 * Chứa JWT Bearer token và thời gian hiệu lực theo chuẩn OpenAPI.
 */
public record LoginResponse(
        // Chuỗi JWT token dùng để xác thực các API quản trị viên (Admin)
        String accessToken,

        // Loại token xác thực (luôn là "Bearer")
        String tokenType,

        // Số giây token còn hiệu lực (tính từ thời điểm phát hành)
        long expiresIn
) {
    /**
     * Phương thức tiện ích để khởi tạo LoginResponse với loại token mặc định là Bearer.
     *
     * @param accessToken chuỗi JWT vừa được ký
     * @param expiresIn   thời gian hiệu lực tính bằng giây
     * @return đối tượng LoginResponse hoàn chỉnh
     */
    public static LoginResponse of(String accessToken, long expiresIn) {
        return new LoginResponse(accessToken, "Bearer", expiresIn);
    }
}
