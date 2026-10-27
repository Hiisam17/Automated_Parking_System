package com.ktpm.parking.api.controller;

import com.ktpm.parking.api.dto.ParkingSessionPageResponse;
import com.ktpm.parking.api.dto.ParkingSessionResponse;
import com.ktpm.parking.business.domain.ParkingSessionStatus;
import com.ktpm.parking.infrastructure.service.ParkingSessionAdminService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

/**
 * REST Controller quản lý và tra cứu các phiên gửi xe dành cho Quản trị viên (Admin).
 * Toàn bộ các API trong controller này yêu cầu xác thực JWT Bearer Token với quyền ROLE_ADMIN.
 */
@Validated
@RestController
@RequestMapping("/api/v1/parking-sessions")
public class ParkingSessionController {

    private final ParkingSessionAdminService parkingSessionAdminService;

    public ParkingSessionController(ParkingSessionAdminService parkingSessionAdminService) {
        this.parkingSessionAdminService = parkingSessionAdminService;
    }

    /**
     * API Xem và lọc danh sách các phiên gửi xe có phân trang.
     * Endpoint: GET /api/v1/parking-sessions
     * Kết quả được sắp xếp mặc định: checkInAt giảm dần, sau đó id giảm dần.
     *
     * @param status       lọc theo trạng thái ACTIVE hoặc COMPLETED (tùy chọn)
     * @param licensePlate lọc theo biển số xe (tùy chọn)
     * @param from         thời gian checkInAt bắt đầu lọc (ISO 8601, tùy chọn)
     * @param to           thời gian checkInAt kết thúc lọc (ISO 8601, tùy chọn)
     * @param page         trang bắt đầu từ 0 (mặc định 0)
     * @param size         số bản ghi mỗi trang, tối đa 100 (mặc định 20)
     * @return HTTP 200 OK kèm ParkingSessionPageResponse
     */
    @GetMapping
    public ResponseEntity<ParkingSessionPageResponse> listParkingSessions(
            @RequestParam(required = false) ParkingSessionStatus status,
            @RequestParam(required = false) String licensePlate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        ParkingSessionPageResponse response = parkingSessionAdminService.listParkingSessions(
                status,
                licensePlate,
                from,
                to,
                page,
                size
        );
        return ResponseEntity.ok(response);
    }

    /**
     * API Xem chi tiết một phiên gửi xe theo ID.
     * Endpoint: GET /api/v1/parking-sessions/{id}
     *
     * @param id ID của phiên gửi xe cần tra cứu
     * @return HTTP 200 OK kèm ParkingSessionResponse nếu tìm thấy;
     *         HTTP 404 SESSION_NOT_FOUND nếu không tìm thấy phiên.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ParkingSessionResponse> getParkingSession(@PathVariable @Min(1) Long id) {
        ParkingSessionResponse response = parkingSessionAdminService.getParkingSession(id);
        return ResponseEntity.ok(response);
    }
}
