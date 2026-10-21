package com.ktpm.parking.business.service;

public enum ParkingError {
    VALIDATION_ERROR("Dữ liệu đầu vào không hợp lệ."),
    VEHICLE_ALREADY_PARKED("Xe này đã có phiên gửi xe đang hoạt động."),
    NO_AVAILABLE_SPOT("Không còn chỗ phù hợp với loại xe."),
    NO_ACTIVE_SESSION("Không có phiên gửi xe đang hoạt động cho biển số này."),
    EVENT_ID_REUSED("eventId đã được dùng với dữ liệu khác.");

    private final String message;

    ParkingError(String message) {
        this.message = message;
    }

    public String code() {
        return name();
    }

    public String message() {
        return message;
    }
}
