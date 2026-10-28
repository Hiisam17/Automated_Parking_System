package com.ktpm.parking.api.dto;

import com.ktpm.parking.business.service.PageResult;

/**
 * DTO chứa thông tin metadata phân trang dùng chung cho các API có phân trang.
 * Khớp với PageMeta schema trong đặc tả OpenAPI.
 */
public record PageMeta(
        // Chỉ số trang hiện tại (bắt đầu từ 0)
        int page,

        // Số lượng phần tử tối đa trên mỗi trang
        int size,

        // Tổng số phần tử tìm thấy thỏa mãn điều kiện lọc
        long totalElements,

        // Tổng số trang
        int totalPages
) {
    /**
     * Phương thức tiện ích để tính toán thông tin phân trang từ danh sách và tham số trang.
     */
    public static PageMeta of(int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageMeta(page, size, totalElements, totalPages);
    }

    public static PageMeta from(PageResult<?> result) {
        return new PageMeta(result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
